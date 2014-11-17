package com.deblox.docker;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Kegan Holtzhausen on 29/05/14.
 *
 * This loads the config and then starts the main application
 *
 */
public class Boot extends Verticle {

    JsonObject config;
    private Logger logger;

    @Override
    public void start(final Future<Void> startedResult) {
        logger = container.logger();
        config = this.getContainer().config();

        logger.info("DockerMod>Boot: Config: " + config.toString());
        logger.info("DockerMod>Boot: Booting Main: " + config.getString("main"));

        container.deployVerticle(config.getString("main", "com.deblox.docker.DockerMod"), config, new AsyncResultHandler<String>() {

            public void handle(AsyncResult<String> deployResult) {
                if (deployResult.succeeded()) {
                    logger.info("DockerMod>Boot: deployed main!");
                } else {
                    logger.error("DockerMod>Boot: deploying module, " + deployResult.cause());
                    startedResult.setFailure(deployResult.cause());
                }
            }

        });

        // services
        for (Object service: config.getArray("services", new JsonArray())) {
            logger.info("DockerMod>Boot: Starting service: " + service);
            container.deployVerticle(service.toString(), config, new AsyncResultHandler<String>() {

                public void handle(AsyncResult<String> deployResult) {
                    if (deployResult.succeeded()) {
                        logger.info("DockerMod>Boot: deployed service");
                    } else {
                        logger.error("DockerMod>Boot: deploying service failed, " + deployResult.cause());
                        startedResult.setFailure(deployResult.cause());
                    }
                }

            });
        }
    }
}