package Worker;

import Utils.config;
import Utils.sqsMessage;
import Utils.sqsUtil;
import Utils.visibilityChanger;
import com.asprise.ocr.Ocr;
import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

public class Worker {

    public static String parseImage(String imageURL){
        try {
                Ocr.setUp();
                Ocr ocr = new Ocr();
                ocr.startEngine("eng",Ocr.SPEED_SLOW);
                URL url = new URL(imageURL);
                BufferedImage img = ImageIO.read(url);
                String result = ocr.recognize(img,Ocr.RECOGNIZE_TYPE_ALL,Ocr.OUTPUT_FORMAT_PLAINTEXT);
                return result;

            } catch (Exception e) { // problem with reading the file
                return e.getMessage();
            }
    }

    // @TODO: WHAT IS THE EXTENDER LULW
    public static void main(String[] args) throws InterruptedException {
        Thread visibilityThread = null;
        visibilityChanger theExtender = null;

        while (true) {
            System.out.println("Worker checking for incoming messages");

            List<Message> messages = sqsUtil.receiveMessages(config.manager2WorkersQ, 1);
            if (messages != null && messages.size() > 0) {
                try {
                    theExtender = new visibilityChanger(messages.get(0), config.manager2WorkersQ);
                    visibilityThread = new Thread(theExtender);
                    visibilityThread.start();

                    //construct the received message
                    sqsMessage msg = new Gson().fromJson(messages.get(0).body(), sqsMessage.class);
//                    Job j = new Gson().fromJson(msg.body, Job.class);

                    //initiate parsing operation
                    System.out.println("trying to parse the text file");
                    String res = parseImage(msg.body);

                    //send the results back to manager
                    System.out.println("Message to be sent back:\n" + res + "\n");
                    sqsMessage toSendBack = new sqsMessage("NO_REPLY_NEEDED", msg.body+";"+res, false);
                    sqsUtil.sendMessage(toSendBack, sqsUtil.getQueueUrl(msg.sqsReplyQ));
                    theExtender.terminate = true;
                } catch (Exception e) {
                    System.out.println("Something went wrong in Worker: " + e);
                    e.printStackTrace();
                    if (visibilityThread != null)
                        visibilityThread.interrupt();
                }
            }else {
                Thread.sleep(10000);
            }
        }
    }
}
