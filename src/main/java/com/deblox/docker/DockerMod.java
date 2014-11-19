package com.deblox.docker;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.VoidHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;


/**
 * Created by keghol on 12/2/13.
 */

public class DockerMod extends BusModBase implements Handler<Message<JsonObject>> {

    private HttpClient client;
    private Logger logger;
    private String hostname;
    private Set<String> docks; // store information about other instances of dockermod
    private ConcurrentMap<String, String> containers;
    private String clusterAddress;
    private String localAddress;
    private EventBus eb;
    private long taskTimeout;
    private long newContainerTimeout;

    // shutdown hook
    public void stop() {
        logger.info("DockerMod Shutting Down, docker instances will remain running");
        eb.publish(clusterAddress + ".register", new JsonObject()
                .putString("action", "unregister")
                .putString("hostname", hostname));
        super.stop();
    }

    // called when we want to subscribe to eventbus
	private void registerHandler(String address) {
		logger.info("DockerMod registering with queue: " + address);
		eb.registerHandler(address, this);
	}

    // called when we want to subscribe to eventbus
    private void unRegisterHandler(String address) {
        logger.info("DockerMod unregistering queue: " + address);
        eb.unregisterHandler(address, this);
    }


    // debug stuff
    private void dumpSets() {
        //good way:
        Iterator<String> iterator = docks.iterator();
        while(iterator.hasNext()) {
            logger.info("docks: " + iterator.next());
        }

        for (Map.Entry<String,String> e: containers.entrySet()) {
            logger.info("containers entry: " + e.getKey() + " contents: " + e.getValue());
        }

    }

    @Override
    public void start() {
        /*

        Startup, read config data / set defaults, call registerHandler

         */
        super.start();
        logger = Logger.getLogger("dockermod");
        logger.info("DockerMod Starting...");
        logger.info("Setting up eventbus");
        eb = vertx.eventBus();

        // store containers and dockermod cluster info in a shared map
        docks =  vertx.sharedData().getSet("docks");
        containers = vertx.sharedData().getMap("deblox.containers");


        // Figure out the hostname so we can subscribe to the private QUEUE for this instance!
        try {
            hostname = getOptionalStringConfig("hostname", InetAddress.getLocalHost().getHostName());
            logger.info("DockerMod machine hostname: " + hostname);
        } catch (UnknownHostException e) {
            logger.warning("unable to determine hostname, this is bad! either pass a config param for Hostname or fix getHostName method to support this OS");
            e.printStackTrace();
            super.stop();
        }

        // queues and topics
        clusterAddress = getOptionalStringConfig("clusterAddress", "deblox.docker");
        localAddress = clusterAddress + "." + hostname;
        logger.info("clusterAddress: " + clusterAddress);
        logger.info("localAddress: " + localAddress);

        // performance settings
        taskTimeout = getOptionalLongConfig("taskTimeout", 250); // timeout when distributing tasks to other instances of dockerMod
        logger.info("taskTimeout: " + taskTimeout);
        newContainerTimeout = getOptionalLongConfig("newContainerTimeout", 15000);

        // subscribe
        logger.info("DockerMod registering handlers");
        registerHandler(clusterAddress);
        registerHandler(localAddress);
        registerHandler(clusterAddress + ".register");

        // which docker daemon to connect to
        String dockerHost = getOptionalStringConfig("dockerHost", "app0126.proxmox.swe1.unibet.com");
		logger.info("dockerHost: " + dockerHost);
        Integer dockerPort = getOptionalIntConfig("dockerPort", 5555);
		logger.info("dockerPort: " + dockerPort);

        // actual dockerd client connection
        client = vertx.createHttpClient()
                .setPort(dockerPort)
                .setHost(dockerHost)
                .setMaxPoolSize(10);
		logger.info("DockerMod connected to the docker daemon");
		
		// the interval between cluster announcements
		Integer announceInterval = getOptionalIntConfig("announceInterval", 1000);
		logger.info("DockerMod announceInterval: " + announceInterval );
		
        // Publish a notification to the cluster to register myself periodically
        long announceTimer = vertx.setPeriodic(announceInterval, new Handler<Long>() {
            public void handle(Long timerID) {
//                logger.info("DockerMod sending register event to: " + clusterAddress + ".register");
                eb.publish(clusterAddress + ".register", new JsonObject()
                        .putString("action", "register")
                        .putString("hostname", hostname));
            }
        });


        Integer trackingServiceInterval = getOptionalIntConfig("trackingServiceInterval", 10000);
        logger.info("DockerMod trackingServiceInterval: " + announceInterval );
        long trackingServiceTimer = vertx.setPeriodic(trackingServiceInterval, new Handler<Long>() {
            public void handle(Long timerID) {
//                logger.info("Calling Tracking Service");
                doContainerTrackingUpdate();
                dumpSets();
            }
        });

        logger.info("DockerMod Startup complete");
    }

    @Override
    public void handle(final Message<JsonObject> message) {
//        logger.info("DockerMod Got message: " + message.body());
//        logger.info("Address: " + message.address());

        final String action = getMandatoryString("action", message);
        JsonObject body = message.body().getObject("body", new JsonObject());

        // defaults
        String method = "GET";
        String url = "";

        // Create initial headers
        JsonObject headers = message.body()
                .getObject("headers", new JsonObject()
                        .putString("Accept", "application/json")
                        .putString("Content-Type", "application/json")
                );
        Map map = headers.toMap();

        switch (action)
        {
            case "register":
                doRegisterDocker(message);
                break;

            case "unregister":
                doUnregisterDocker(message);
                break;

            case "list-containers":
                // should request list of containers from entire cluster, then respond TODO FIXME

                // if all is false, just respond with this nodes list
                if ( ! message.body().getBoolean("all", false)) {
                    url = "/containers/json?all=1"; // all in here is for running / stopped containers also!
                    doHttpRequest(method, url, map, body, message);
                } else {
                    // query the cluster, give back a larger response
                    message.body().removeField("all"); // strip to avoid inception
                    Integer count = docks.size(); // set the number of responses we expect
                    logger.info("Querying entire cluster of " +count+ " nodes");
                    final ResponseConcentrator rc = new ResponseConcentrator();
                    rc.setExpectedResponseCount(count);
                    rc.setOriginalMessage(message);

                    Iterator d = docks.iterator();
                    while (d.hasNext()) {
                        eb.sendWithTimeout(clusterAddress + "." + d.next(), message.body(), taskTimeout, new Handler<AsyncResult<Message<JsonObject>>>() {
                            @Override
                            public void handle(AsyncResult<Message<JsonObject>> event) {
                                if (event.succeeded()) {
                                    rc.resultUpdate(event.result().body());
                                } else {
                                    logger.warning("Error, bus timeout");
                                    rc.resultUpdate(new JsonObject().putString("error", "timeout"));
                                }
                            }
                        });
                    }

                }
                break;

            case "list-images":
                url = "/images/json";
                doHttpRequest(method, url, map, body, message);
                break;

            case "create-raw-container":
                body = getMandatoryObject("body", message);
                logger.info("Creating raw container with body: " + body);
                doHttpRequest(method, url, map, body, message);
                break;

            case "shutdown-container":
                // lb calls
                // shutdown call
                // remove
                break;

            case "restart-container":
                logger.info("Restarting container: " + getMandatoryString("id", message));
                url = "/containers/" + getMandatoryString("id", message) + "/restart";
                method = "POST";
                doHttpRequest(method, url, map, body, message);
                break;

            case "stop-container":
                logger.info("Stopping container: " + getMandatoryString("id", message));
                url = "/containers/" + getMandatoryString("id", message) + "/kill";
                method = "POST";
                doHttpRequest(method, url, map, body, message);
                break;

            case "start-container":
                logger.info("Starting container: " + getMandatoryString("id", message));
                url = "/containers/" + getMandatoryString("id", message) + "/start";
                method = "POST";
                doHttpRequest(method, url, map, body, message);
                break;

            case "inspect-container":
                String imageId = getMandatoryString("id", message);
                url = "/containers/" + imageId + "/json";
                doHttpRequest(method, url, map, body, message);
                break;

            case "create-container":
                // check if template is specified
                if (message.body().containsField("template")) {
                    logger.info("attempting to load template: " + getMandatoryString("template", message));
                    // load the son template, TODO make the path a configurable
                    try {
                        body = Util.loadConfig(this, "/templates/" + getMandatoryString("template", message) + ".json");
                    } catch (IOException e) {
                        logger.warning("unable to open template, does it exist?");
                        e.printStackTrace();
                    }
                } else {
                    body = new NewContainerBuilder().createC(getMandatoryString("image", message)).toJson();
                }

                logger.info("Creating container: " + body.toString());
                url = "/containers/create";
                method = "POST";

                // if the message containes the field instances, prepare to spawn many across the cluster
                if (message.body().containsField("instances")) {

                    logger.info("Creating multiple containers because instances is present");

                    // copy the docks list of other docker instances
                    Set<String> tmpDocks = docks; // copy the docks list
                    final Iterator<String> d = tmpDocks.iterator(); // randomize how TODO FIXME

                    // for count loop
                    Integer count = message.body().getInteger("instances", 1);
                    if (count == 0) { count =1; }
                    Integer x;

                    // remove instances from the original message to prevent loop
                    message.body().removeField("instances");

                    // concentrator of responses + callback handler
                    final ResponseConcentrator rc = new ResponseConcentrator();
                    rc.setExpectedResponseCount(count);
                    rc.setOriginalMessage(message);

                    // loop for the desired count
                    for (x=1; x<=count; x++) {

                        // set the address to the next node in the cluster
                        String address;
                        try {
                            address = clusterAddress + "." + d.next();
                        } catch (NoSuchElementException e) {
                            logger.warning("No more nodes, defaulting to the cluster in its entirety");
                            address = clusterAddress;
                        }

                        eb.sendWithTimeout(address, message.body(), newContainerTimeout, new Handler<AsyncResult<Message<JsonObject>>>() {
                            @Override
                            public void handle(AsyncResult<Message<JsonObject>> event) {
                                if (event.succeeded()) {
                                    rc.resultUpdate(event.result().body());
                                } else {
                                    logger.warning("Error, bus timeout");
                                    rc.resultUpdate(new JsonObject().putString("error", "timeout"));
                                }
                            }
                        });
                    }

                } else {
                    doAsyncHttpRequest(method, url, map, body, message, new Handler<JsonObject>() {
                        @Override
                        public void handle(final JsonObject httpevent) {
                            logger.info("Async Create Container Result: " + httpevent.toString());

                            // Register the container queue
                            try {
                                registerHandler(httpevent.getObject("Response").getObject("Body").getString("Id"));
                            } catch (Exception e) {
                                logger.warning("Unable to create an event listener");
                            }

                            // reply with the resultset
                            message.reply(httpevent);

                        }
                    });
                }
                break;

            case "delete-container":
                method="DELETE";
                String id = getMandatoryString("id", message);
                logger.info("Deleting container id: " + id );
                url = "/containers/" + id;
                doAsyncHttpRequest(method, url, map, body, message, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {
                        logger.info("Completed Delete Event: " + event);
                        // TODO FIXME unregister handler for id
                        unRegisterHandler(getMandatoryString("id", message));
                        message.reply(event);
                    }
                });
                break;

            default:
                message.reply(new JsonObject().putString("error", "no such action is supported"));
                break;
        }
    }

    private void doRegisterDocker(Message<JsonObject> message) {
        doUnregisterDocker(message);
        String hostname = getMandatoryString("hostname", message);
//        logger.info("Registering docker server: " + hostname);
		// call remove twice to de-dup TODO make a periodic health check for docks in list
        docks.remove(hostname);
        docks.add(hostname);
    }

    private void doUnregisterDocker(Message<JsonObject> message) {
        String hostname = getMandatoryString("hostname", message);
//        logger.info("Unregistering docker server: " + hostname);
		// call remove twice to de-dup TODO make a periodic health check for docks in list
        docks.remove(hostname);
    }

    // call the docker and list the containers both running and stopped, and update the local cache
    private void doContainerTrackingUpdate() {
        eb.send(localAddress, new JsonObject().putString("action", "list-containers"), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                System.out.println("Response: " + reply.body());
                Iterator containerIterator = reply.body().getObject("Response").getArray("Body").iterator();

                for ( Iterator<JsonObject>container = containerIterator; containerIterator.hasNext(); ) {
                    JsonObject tc = container.next();
                    logger.info(tc.toString());
                    logger.info("Container Id: " + tc.getString("Id") + ", status: " + tc.getString("Status") );
                    containers.remove(tc.getString("Id"));
                    containers.put(tc.getString("Id"), tc.toString());
                    registerHandler(tc.getString("Id"));
                }

            }
        });
    }


    private void doAsyncHttpRequest(String method, String url, Map headers, JsonObject body, final Message<JsonObject> message, final Handler<JsonObject> callback) {
        HttpClientRequest request = client.request(method, url , new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse httpClientResponse) {

                // response object
                final JsonObject response = new JsonObject();
                // where we store the docker response buffers
                final Buffer dockerResponse = new Buffer();

                logger.info("Docker response code: " + httpClientResponse.statusCode());
                response.putNumber("statusCode", httpClientResponse.statusCode());

                httpClientResponse.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        logger.info("Docker response buffer contents: " + buffer);
                        dockerResponse.appendBuffer(buffer);
                    }
                });

                httpClientResponse.endHandler(new VoidHandler() {
                    @Override
                    protected void handle() {

                        logger.info("Response buffers are all in!");

                        for (Map.Entry<String, String> header : httpClientResponse.headers().entries()) {
                            response.putString(header.getKey(), header.getValue());
                        }

                        try {
                            JsonObject jo = new JsonObject("{\"Body\":" + new String(dockerResponse.getBytes(), "UTF-8") + "}");
                            response.putObject("Response", jo);
                        } catch (UnsupportedEncodingException e) {
                            logger.warning("Unable to read docker response buffer into json response object");
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        catch (DecodeException e) {
                            logger.warning("not a json response,relying on response-code");
                            try {
                                response.putString("Response", new String(dockerResponse.getBytes(), "UTF-8"));
                            } catch (UnsupportedEncodingException e1) {
                                e1.printStackTrace();
                            }
                        }

                        // Put my hostname into the message so we know where it came from, though It probably should not matter.
                        response.putString("dockerInstance", hostname);
//						response.putNumber("statusCode", httpClientResponse.statusCode());
                        logger.info("Generated Response Message: " + response.toString());

                        try {
                            if (callback!=null) {
                                callback.handle(response); // TODO FIXME
                            } else {
                                message.reply(response);
                            }
                        } catch (Exception e) {
                            logger.warning("Unable to respond to this message, I hope thats OK");
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        request.headers().set(headers);
        request.headers().set("Content-Length", String.valueOf(body.toString().length()));
        request.write(body.toString());
        request.end();
    }

    private void doHttpRequest(String method, String url,Map headers,  JsonObject body, final Message<JsonObject> message ) {
        doAsyncHttpRequest(method, url, headers, body, message, null);
	}
}