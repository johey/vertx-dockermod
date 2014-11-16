package com.deblox.docker;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.VoidHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
    private ConcurrentMap<String, JsonObject> containers;
    private String clusterAddress;
    private String localAddress;
    private EventBus eb;


    public void stop() {
        logger.info("DockerMod Shutting Down, docker instances will remain running");
        eb.publish(clusterAddress + ".register", new JsonObject()
                .putString("action", "unregister")
                .putString("hostname", hostname));
        super.stop();
    }
    
	private void registerHandler(String address) {
		logger.info("DockerMod instance registering: " + address);
		eb.registerHandler(address, this);
	}

    private void dumpSets() {
        //good way:
        Iterator<String> iterator = docks.iterator();
        while(iterator.hasNext()) {
            logger.info("docks: " + iterator.next());
        }
    }

    @Override
    public void start() {
        /*

        Startup, read config data / set defaults and subscribe to the messageBus

         */
        docks =  vertx.sharedData().getSet("docks");
        super.start();
        logger = Logger.getLogger("dockermod");
        logger.info("DockerMod Starting...");
        eb = vertx.eventBus();

        // store containers in a shared map
        containers = vertx.sharedData().getMap("deblox.containers");

        // Connect to the shared docker instance directory


        clusterAddress = getOptionalStringConfig("clusterAddress", "deblox.docker");
        localAddress = clusterAddress + "." + hostname;

		logger.info("clusterAddress: " + clusterAddress);
        String dockerHost = getOptionalStringConfig("dockerHost", "app0126.proxmox.swe1.unibet.com");
		logger.info("dockerHost: " + dockerHost);
        Integer dockerPort = getOptionalIntConfig("dockerPort", 5555);
		logger.info("dockerPort: " + dockerPort);

        // Figure out the hostname so we can subscribe to the private QUEUE for this instance!
        try {
            hostname = getOptionalStringConfig("hostname", InetAddress.getLocalHost().getHostName());
			logger.info("DockerMod machine hostname: " + hostname);
        } catch (UnknownHostException e) {
            logger.warning("unable to determine hostname, this is bad! either pass a config param for Hostname or fix getHostName method to support this OS");
            e.printStackTrace();
            super.stop();
        }

        client = vertx.createHttpClient()
                .setPort(dockerPort)
                .setHost(dockerHost)
                .setMaxPoolSize(10);
		logger.info("DockerMod connected to the docker daemon");

		logger.info("DockerMod registering handlers");
		registerHandler(clusterAddress);
        registerHandler(localAddress);

//        if (getOptionalBooleanConfig("registerConsumer", true)) {
//            registerHandler(clusterAddress + ".register"); // address new instances anounce to!
//        }

        logger.info("DockerMod Registering with the DockerMod cluster");
		
		// the interval between cluster announcements
		Integer announceInterval = getOptionalIntConfig("announceInterval", 1000);
		logger.info("DockerMod announceInterval: " + announceInterval );
		
        // Publish a notification to the cluster to register myself periodically
        long announceTimer = vertx.setPeriodic(announceInterval, new Handler<Long>() {
            public void handle(Long timerID) {
                logger.info("Sending register event to: " + clusterAddress + ".register");
                eb.publish(clusterAddress + ".register", new JsonObject()
                        .putString("action", "register")
                        .putString("hostname", hostname));
            }
        });


        Integer trackingServiceInterval = getOptionalIntConfig("trackingServiceInterval", 10000);
        logger.info("DockerMod trackingServiceInterval: " + announceInterval );
        long trackingServiceTimer = vertx.setPeriodic(trackingServiceInterval, new Handler<Long>() {
            public void handle(Long timerID) {
                logger.info("Calling Tracking Service");
                doContainerTrackingUpdate();
            }
        });


        logger.info("DockerMod Startup complete");
    }

    @Override
    public void handle(final Message<JsonObject> message) {
        logger.info("DockerMod Got message: " + message.body());
        logger.info("Address: " + message.address());

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
                url = "/containers/json?all=1";
                doHttpRequest(method, url, map, body, message);
                break;
            case "list-images":
                url = "/images/json";
                doHttpRequest(method, url, map, body, message);
                break;
            case "create-container":
                body = new NewContainerBuilder().createC( getMandatoryString("image", message) ).toJson();
                logger.info("Creating instance: " + body.toString());
                url = "/containers/create";
                method = "POST";
                doHttpRequest(method, url, map, body, message);
                break;
            case "create-raw-container":
                body = getMandatoryObject("body", message);
                doHttpRequest(method, url, map, body, message);
            case "create-unibet-container":
                // Create a container from a JSON template
                logger.info("attempting to load template: " + getMandatoryString("template", message));
                try {
                    body = Util.loadConfig(this, "/templates/" + getMandatoryString("template", message) + ".json");
                } catch (IOException e) {
                    logger.warning("unable to open template, does it exist?");
                    e.printStackTrace();
                }
                //body = read file
                logger.info("Creating templated instance: " + body.toString());
                url = "/containers/create";
                method = "POST";
                doHttpRequest(method, url, map, body, message);
                break;
            case "start-container":
                logger.info("Starting instance: " + getMandatoryString("id", message));
                url = "/containers/" + getMandatoryString("id", message) + "/start";
                method = "POST";
                doHttpRequest(method, url, map, body, message);
                break;
            case "inspect-container":
                String imageId = getMandatoryString("id", message);
                url = "/containers/" + imageId + "/json";
                doHttpRequest(method, url, map, body, message);
                break;
        }
    }

    private void doRegisterDocker(Message<JsonObject> message) {
        doUnregisterDocker(message);
        String hostname = getMandatoryString("hostname", message);
        logger.info("Registering docker server: " + hostname);
		// call remove twice to de-dup TODO make a periodic health check for docks in list
        docks.remove(hostname);
        docks.add(hostname);
        dumpSets();
    }

    private void doUnregisterDocker(Message<JsonObject> message) {
        String hostname = getMandatoryString("hostname", message);
        logger.info("Unregistering docker server: " + hostname);
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
                }

            }
        });
    }

    private void doHttpRequest(String method, String url,Map headers,  JsonObject body,  final Message<JsonObject> message ) {
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
							message.reply(response);
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
}