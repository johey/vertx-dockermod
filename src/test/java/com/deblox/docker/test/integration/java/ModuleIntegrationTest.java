package com.deblox.docker.test.integration.java;/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.testtools.TestVerticle;
import org.vertx.java.core.json.JsonObject;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.vertx.testtools.VertxAssert.*;

/**
 * Example Java integration test that deploys the module that this project builds.
 *
 * Quite often in integration tests you want to deploy the same module for all tests and you don't want tests
 * to start before the module has been deployed.
 *
 * This test demonstrates how to do that.
 */
public class ModuleIntegrationTest extends TestVerticle {

    @Test
    public void testListContainers() {
        container.logger().info("testListContainers");
        vertx.eventBus().send("deblox.docker", new JsonObject().putString("action", "list-containers"), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                System.out.println("Response: " + reply.body());
                JsonArray body = reply.body().getObject("Response").getArray("Body");
                assertNotNull(body);
                testComplete();
            }
        });
    }

    @Test
    public void testListImages() {
        container.logger().info("testListImages");
        vertx.eventBus().send("deblox.docker", new JsonObject().putString("action", "list-images"), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                System.out.println("Response: " + reply.body());
                JsonArray body = reply.body().getObject("Response").getArray("Body");
                assertNotNull(body);
                testComplete();
            }
        });
    }

    @Test
    public void testNewContainer() {
        container.logger().info("testNewContainer");

        JsonObject request = new JsonObject().putString("action", "create-container")
                                             .putString("image", "ubuntu");

        vertx.eventBus().send("deblox.docker", request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                try {
                System.out.println("Response: " + reply.body());
                JsonObject body = reply.body().getObject("Response").getObject("Body");
                assertNotNull(body.getString("Id"));
                testComplete();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        });
    }


    @Test
    public void testStartContainer() {
        container.logger().info("testStartContainer");

        JsonObject request = new JsonObject().putString("action", "create-container")
                .putString("image", "ubuntu");

        vertx.eventBus().send("deblox.docker", request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                try {
                    System.out.println("Response: " + reply.body());
                    JsonObject body = reply.body().getObject("Response").getObject("Body");
                    assertNotNull(body.getString("Id"));

                    // start the container
                    JsonObject request = new JsonObject().putString("action", "start-container")
                            .putString("id", body.getString("Id"));


                    vertx.eventBus().send("deblox.docker", request, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> reply) {
                            try {
                                System.out.println("Response: " + reply.body());
//                                JsonObject body = reply.body().getObject("Response").getObject("Body");
                                assertNotNull(reply.body().getNumber("statusCode"));
                                testComplete();

                            } catch (Exception e) {
                                e.printStackTrace();
                                throw e;
                            }
                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        });
    }


    @Test
    public void testInspectContainer() throws UnknownHostException {
        container.logger().info("testInspectContainer");

        JsonObject request = new JsonObject().putString("action", "inspect-container")
                .putString("id", "foo");

        String hostname = InetAddress.getLocalHost().getHostName();

                vertx.eventBus().send("deblox.docker." + hostname, request, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> reply) {
                        System.out.println("Response: " + reply.body());
                        Number rcode = reply.body().getNumber("statusCode");
                        assertEquals(rcode, 404);
                        testComplete();
                    }
                });
    }

    @Override
    public void start() {

        initialize();

        container.deployModule(System.getProperty("vertx.modulename"), new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {

                // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
                if (asyncResult.failed()) {
                    container.logger().error(asyncResult.cause());
                }

                assertTrue(asyncResult.succeeded());
                assertNotNull("deploymentID should not be null", asyncResult.result());

                // If deployed correctly then start the tests!
                startTests();
            }
        });
    }

}
