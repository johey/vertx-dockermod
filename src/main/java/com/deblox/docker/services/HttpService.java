package com.deblox.docker.services;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class HttpService extends BusModBase {
    private int SERVER_PORT;
    private Logger logger;
    protected EventBus eventBus;
    private RouteMatcher routeMatcher;
    protected JsonObject message = new JsonObject();

    private String clusterAddress;
    private String localAddress;
    private String hostname;

    @Override
    public void start() {
        super.start();
        logger = Logger.getLogger("HttpService");
        logger.info("HttpService starting up");
        eventBus = vertx.eventBus();
        routeMatcher = new RouteMatcher();

        // Figure out the hostname so we can subscribe to the private QUEUE for this instance!
        try {
            hostname = getOptionalStringConfig("hostname", InetAddress.getLocalHost().getHostName());
            logger.info("DockerMod machine hostname: " + hostname);
        } catch (UnknownHostException e) {
            logger.warning("unable to determine hostname, this is bad! either pass a config param for Hostname or fix getHostName method to support this OS");
            e.printStackTrace();
            super.stop();
        }

        clusterAddress = getOptionalStringConfig("clusterAddress", "deblox.docker");
        localAddress = clusterAddress + "." + hostname;

        SERVER_PORT = getOptionalIntConfig("restServicePort", 8080);
        logger.info("HttpService listening on port " + SERVER_PORT);

        routeMatcher.post("/", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        JsonObject document = new JsonObject(event.toString());
                        String address = clusterAddress;

                        // if start-container is requested, set the queue to the instance ID
                        switch(document.getString("action")) {
                            case "start-container":
                                logger.info("Setting queue to instance ID: " +document.getString("id") );
                                address = document.getString("id");
                        }

                        logger.info("HttpService got rest request " + document.toString());
                        logger.info("HttpService Forwarding request to cluster...");
                        eventBus.send(address, document, new Handler<Message<JsonObject>>() {
                            public void handle(Message<JsonObject> message) {
                                logger.info("HttpService got response from MSGBUS: " + message.body().toString());
                                if (message.body().getString("status")!="error") {
                                    logger.info("HttpService got response");
                                    req.response().end(message.body().toString());
                                } else {
                                    logger.warning("HttpService no response");
                                    req.response().end("FAILURE: " + message.body());
                                }

                            }
                        });
                    }
                });
            }
        });

        routeMatcher.get("/", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                logger.info("HttpService works on POST requests, there is no reason to GET this URL");
                req.response().end("DockerMod HttpService");
            }
        });

        HttpServer server = vertx.createHttpServer().requestHandler(routeMatcher).setAcceptBacklog(50000).listen(SERVER_PORT);

    }
}