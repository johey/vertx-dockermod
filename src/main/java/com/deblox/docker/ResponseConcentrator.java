package com.deblox.docker;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by keghol on 18/11/14.
 *
 * awats a certin number of "callbacks" and then responds to a message with results from each callee.
 *
 * instantiates this with a count, and a original message, each subsequent eventbus message
 * whos response you are interrested in should call this.resultUpdate
 *
 * e.g:
 final ResponseConcentrator rc = new ResponseConcentrator();
 rc.setExpectedResponseCount(count);
 rc.setOriginalMessage(message);

 eb.send(address, message.body(), new Handler<Message<JsonObject>>() {
    @Override
        public void handle(Message<JsonObject> event) {
          rc.resultUpdate(event.body());
        }
});

 */
public class ResponseConcentrator {
    private Integer expectedResponseCount; // number of responses expected
    private JsonArray resultArray = new JsonArray(); // array to store results in from callee's
    private Message originalMessage; // the original message we will reply to with all the results

    public void setOriginalMessage(Message originalMessage) {
        this.originalMessage = originalMessage;
    }

    public void setExpectedResponseCount(Integer expectedResponseCount) {
        this.expectedResponseCount = expectedResponseCount;
    }

    public void resultUpdate(JsonObject r) {
        resultArray.add(r);
        if (resultArray.size() >= expectedResponseCount) {
            originalMessage.reply(new JsonObject().putArray("containers", resultArray));
        }
    }

}
