package com.deblox.docker.services;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Created by keghol on 16/11/14.
 */
public class ContainerTrackingService extends BusModBase implements Handler<Message<JsonObject>> {

    private Logger logger;
    private ConcurrentMap<String, String> containers;
    private String clusterAddress;
    private String localAddress;
    private EventBus eb;

    @Override
    public void start() {
        super.start();
        logger = Logger.getLogger("ContainerTrackingService");
        logger.info("Starting ContainerTrackingService");
    }

    @Override
    public void handle(Message<JsonObject> event) {

    }
}
