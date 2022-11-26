package Manager;

import Utils.*;
import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Manager {
    static boolean terminate = false;

    public static void main(String[] args) {
        System.out.println("Starting Manager");

        sqsUtil.createSqsQueue(config.manager2WorkersQ);
        ExecutorService pool = Executors.newFixedThreadPool(4);

        ec2Utils.startEc2s("Worker", bashCommand.initEmployee, 1);
        System.out.println("Manager Created 1st Worker, Good!");

        while (true) {
            List<Message> inbox;
            if (terminate) {
                pool.shutdown();
                while (!pool.isTerminated()) {
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //delete all resources
                sqsUtil.deleteQueue(config.localToManager);
                sqsUtil.deleteQueue(config.manager2WorkersQ);
                ec2Utils.terminateAll("Worker");
                ec2Utils.terminateAll("Manager");
                break;
            }
            inbox = sqsUtil.receiveMessages(config.localToManager, 1);
            if (inbox != null && inbox.size() != 0) {
                sqsMessage sqsmessage = new Gson().fromJson(inbox.get(0).body(), sqsMessage.class);
                terminate = sqsmessage.terminate;

                ManagerTask task = new ManagerTask(sqsmessage);
                sqsUtil.deleteMessage(inbox.get(0), config.localToManager);
                pool.execute(task);
            }
        }
    }
}
