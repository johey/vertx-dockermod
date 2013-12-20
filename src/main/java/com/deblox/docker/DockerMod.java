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
import java.util.Map;
import java.util.logging.Logger;


/**
 * Created by keghol on 12/2/13.
 */

public class DockerMod extends BusModBase implements Handler<Message<JsonObject>> {

    private HttpClient client;
    private Logger logger;
    private String clusterAddress;
    private String dockerHost;
    private Integer dockerPort;

    @Override
    public void start() {
        /*

        Startup, read config data / set defaults and subscribe to the messageBus

         */

        super.start();
        logger = Logger.getLogger("dockermod");
        logger.info("Starting...");

        this.clusterAddress = getOptionalStringConfig("clusterAddress", "deblox.docker");
        this.dockerHost = getOptionalStringConfig("dockerHost", "localhost");
        this.dockerPort = getOptionalIntConfig("dockerPort", 5555);

        client = vertx.createHttpClient()
                .setPort(this.dockerPort )
                .setHost(this.dockerHost )
                .setMaxPoolSize(10);

        vertx.eventBus().registerHandler(this.clusterAddress, this);

        logger.info("Registered");
    }

    @Override
    public void handle(final Message<JsonObject> message) {
        logger.info("Got message: " + message.body());

        final String action = getMandatoryString("action", message);
        JsonObject body = message.body().getObject("body", new JsonObject());
        //String bodyLength = ;

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

            case "list-containers":
                url = "/containers/json";
                break;
            case "list-images":
                url = "/images/json";
                break;
            case "create-container":
                url = "/containers/create";
                method = "POST";
                String image = getMandatoryString("image", message);
                body = new NewContainerBuilder().createC(image).toJson();
                logger.info("Generated body: " + body.toString());
                break;
        }



        HttpClientRequest request = client.request(method, url, new Handler<HttpClientResponse>() {



            @Override
            public void handle(final HttpClientResponse httpClientResponse) {

                logger.info("Got a response: " + httpClientResponse.statusCode());
                httpClientResponse.dataHandler(new Handler<Buffer>() {

                    @Override
                    public void handle(Buffer buffer) {
                        logger.info("response Buffer: " + buffer);
                        JsonObject response = new JsonObject();

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

                        logger.info("Generated Response Message: " + response.toString());

                        message.reply(response);

                    }
                });
            }
        });


        logger.info("Preparing request which should have the body:" + body);

        request.headers().set(map);
        request.headers().set("Content-Length", String.valueOf(body.toString().length()));
        request.write(body.toString());

        logger.info("Request that will be sent:" + request);

        request.end();


        logger.info("Url" + url);
        logger.info("method" + method);


    }





}