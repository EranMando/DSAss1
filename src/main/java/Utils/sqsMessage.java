package Utils;

import com.google.gson.Gson;

public class sqsMessage {
    public String sqsReplyQ;
    public String body;
    public Boolean terminate;
    public int workerImageRatio;

    public sqsMessage(String sqsReplyQ, String body, Boolean terminate){
        this.sqsReplyQ = sqsReplyQ;
        this.body = body;
        this.terminate = terminate;
    }

    public String toString(){
        return new Gson().toJson(this);
    }
}
