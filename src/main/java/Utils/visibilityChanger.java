package Utils;

import software.amazon.awssdk.services.sqs.model.Message;

public class visibilityChanger implements Runnable {
    private Message msg;
    public volatile boolean terminate = false;
    private String queueInUse;

    public visibilityChanger(Message mess, String queueName) {
        this.queueInUse = queueName;
        this.msg = mess;
    }

    public void run() {
        while(!this.terminate) {
            sqsUtil.changeVisibilityTime(sqsUtil.getQueueUrl(this.queueInUse), this.msg, 25);

            try {
                Thread.sleep(10000L);
            } catch (InterruptedException var2) {
                var2.printStackTrace();
            }
        }

        sqsUtil.deleteMessage(this.msg, this.queueInUse);
    }
}
