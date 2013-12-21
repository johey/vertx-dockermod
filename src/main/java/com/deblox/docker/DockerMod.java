package com.deblox.docker;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


/**
 * Created by keghol on 12/2/13.
 */

public class DockerMod extends BusModBase implements Handler<Message<JsonObject>> {

    private HttpClient client;
    private Logger logger;
    private String hostname;
    private Set<String> docks;
    private String clusterAddress;


    public void stop() {
        logger.info("Shutting down...");
        vertx.eventBus().publish(clusterAddress + ".register", new JsonObject()
                .putString("action", "unregister")
                .putString("hostname", hostname));
        super.stop();
    }
    
    @Override
    public void start() {
        /*

        Startup, read config data / set defaults and subscribe to the messageBus

         */
        docks =  vertx.sharedData().getSet("docks");
        super.start();
        logger = Logger.getLogger("dockermod");
        logger.info("Starting...");

        // Connect to the shared docker instance directory


        clusterAddress = getOptionalStringConfig("clusterAddress", "deblox.docker");
        String dockerHost = getOptionalStringConfig("dockerHost", "localhost");
        Integer dockerPort = getOptionalIntConfig("dockerPort", 5555);

        // Figure out the hostname so we can subscribe to the private QUEUE for this instance!
        try {
            hostname = getOptionalStringConfig("hostname", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            logger.warning("unable to determine hostname, this is bad! either pass a config param for Hostname or fix getHostName method to support this OS");
            e.printStackTrace();
            super.stop();
        }

        client = vertx.createHttpClient()
                .setPort(dockerPort)
                .setHost(dockerHost)
                .setMaxPoolSize(10);

        vertx.eventBus().registerHandler(clusterAddress, this);
        vertx.eventBus().registerHandler(clusterAddress + "." + hostname, this);
        vertx.eventBus().registerHandler(clusterAddress + ".register", this); // address new instances anounce to!

        logger.info("Registering with the cluster");

        // Publish a notification to the cluster to register myself
        long timerID = vertx.setPeriodic(25, new Handler<Long>() {
            public void handle(Long timerID) {
                vertx.eventBus().publish(clusterAddress + ".register", new JsonObject()
                        .putString("action", "register")
                        .putString("hostname", hostname));
            }
        });

        logger.info("Up and Registered");
    }

    @Override
    public void handle(final Message<JsonObject> message) {
        logger.info("Got message: " + message.body());

        final String action = getMandatoryString("action", message);
        JsonObject body = message.body().getObject("body", new JsonObject());

        // defaults
        String method = "GET";
        String url = "";

        // Create initial headers
        JsonObject headers = message.body()
                .getObject("headers", new JsonObject()
                        .putString("Accept", "application/json")
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
                url = "/containers/json";
                doHttpRequest(method, url, map, body, message);
                break;
            case "list-images":
                url = "/images/json";
                doHttpRequest(method, url, map, body, message);
                break;
            case "create-container":
                String image = getMandatoryString("image", message);
                body = new NewContainerBuilder().createC(image).toJson();
                url = "/containers/create";
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
        logger.info("Registering docker server:" + hostname);
        String hostname = getMandatoryString("hostname", message);
        docks.remove(hostname);
        docks.add(hostname);
    }

    private void doUnregisterDocker(Message<JsonObject> message) {
        logger.info("Unregistering docker server:" + hostname);
        String hostname = getMandatoryString("hostname", message);
        docks.remove(hostname);
    }

    private void doHttpRequest(String method, String url,Map headers,  JsonObject body,  final Message<JsonObject> message ) {
        HttpClientRequest request = client.request(method, url , new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse httpClientResponse) {

                logger.info("Got a response: " + httpClientResponse.statusCode());
                httpClientResponse.dataHandler(new Handler<Buffer>() {

                    @Override
                    public void handle(Buffer buffer) {
                        logger.info("response Buffer: " + buffer);
                        JsonObject response = new JsonObject();
                        InetAddress addr = null;

                        for (Map.Entry<String, String> header : httpClientResponse.headers().entries()) {
                            response.putString(header.getKey(), header.getValue());
                        }

                        String rbody = null;
                        try {
                            rbody = new String(buffer.getBytes(), "UTF-8");
                            logger.info("String from buffer:" + rbody);
                        } catch (UnsupportedEncodingException e) {
                            logger.warning("Unable to read httpreponse buffer to String");
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }

                        try {
                            JsonObject jo = new JsonObject("{ \"Body\":" + rbody + "}");
                            response.putObject("Response", jo);
                        } catch (Exception e) {
                            logger.warning("Unable to convert httpresponse body to JSON object");
                            logger.warning("Response Body: " + rbody);
                            e.printStackTrace();
                        }

                        // Put my hostname into the message so we know where it came from, though It probably should not matter.
                        response.putString("dockerInstance", hostname);
                        response.putNumber("statusCode", httpClientResponse.statusCode());
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