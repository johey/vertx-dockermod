package com.deblox.docker;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.VoidHandler;

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
        logger.info("DockerMod Shutting Down, docker instances will remain running");
        vertx.eventBus().publish(clusterAddress + ".register", new JsonObject()
                .putString("action", "unregister")
                .putString("hostname", hostname));
        super.stop();
    }
    
	private void registerHandler(String address) {
		logger.info("DockerMod instance registering: " + address);
		vertx.eventBus().registerHandler(address, this);
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

        // Connect to the shared docker instance directory


        clusterAddress = getOptionalStringConfig("clusterAddress", "deblox.docker");
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
        registerHandler(clusterAddress + "." + hostname);
		registerHandler(clusterAddress + ".register"); // address new instances anounce to!

        logger.info("DockerMod Registering with the DockerMod cluster");
		
		// the interval between cluster announcements
		Integer announceInterval = getOptionalIntConfig("announceInterval", 1000);
		logger.info("DockerMod announceInterval: " + announceInterval );
		
        // Publish a notification to the cluster to register myself periodically
        long timerID = vertx.setPeriodic(announceInterval, new Handler<Long>() {
            public void handle(Long timerID) {
                vertx.eventBus().publish(clusterAddress + ".register", new JsonObject()
                        .putString("action", "register")
                        .putString("hostname", hostname));
            }
        });

        logger.info("DockerMod Startup complete");
    }

    @Override
    public void handle(final Message<JsonObject> message) {
        logger.info("DockerMod Got message: " + message.body());

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
                logger.info("Creating instance: " + body.toString());
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
        logger.info("Registering docker server:" + hostname);
        String hostname = getMandatoryString("hostname", message);
		// call remove twice to de-dup TODO make a periodic health check for docks in list
        docks.remove(hostname);
		docks.remove(hostname);
        docks.add(hostname);
		logger.info("Registered DockerMod instances:");
		logger.info(docks.toString());
    }

    private void doUnregisterDocker(Message<JsonObject> message) {
        logger.info("Unregistering docker server:" + hostname);
        String hostname = getMandatoryString("hostname", message);
		// call remove twice to de-dup TODO make a periodic health check for docks in list
        docks.remove(hostname);
		docks.remove(hostname);
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

//                        if (dockerResponse.getBytes().length == 0) {
//                            dockerResponse.appendString("\"No Response\"");
//                        }

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