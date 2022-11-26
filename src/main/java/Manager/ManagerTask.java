package Manager;

import Utils.*;
import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;

public class ManagerTask implements Runnable{
    public volatile static int CurrWorkers = 1;
    public volatile static int MaxWorkers = 15;
    private int random = (int) (Math.random() * 100000);
    final sqsMessage message;

    public ManagerTask(sqsMessage msg) {
        this.message = msg;
    }

    public void notifyWorkerOfJob(String x) {
        sqsMessage msg = new sqsMessage(config.workers2ManagerQ + random, x, false);
        sqsUtil.sendMessage(msg, sqsUtil.getQueueUrl(config.manager2WorkersQ));
    }

    public void run() {
        String InputFromLocal = s3Utils.getObject("InputFile", message.body);
        String[] lines = InputFromLocal.split("\n");
        int NumOfJobs = 0;
        sqsUtil.createSqsQueue(config.workers2ManagerQ + random);

        for (String x : lines) {
            notifyWorkerOfJob(x);
            NumOfJobs++;
            System.out.println("Processing ==> " + x);
        }

        int workersCount = NumOfJobs / message.workerImageRatio;
        if (workersCount > 1) {
            MoreWorkers(workersCount);
        }

        File WorkersOutputFile = new File("WorkersOutput.txt");
        int finishedJobs = 0;
        FileWriter fileWrite = null;
        sqsMessage temp;
        System.out.println("Opening filewriter & waiting for messages from workers...");

        try {
            fileWrite = new FileWriter("WorkersOutput.txt");
        } catch (Exception e) {
            System.out.println("Failed to open/write to WorkersOutput.txt");
            e.printStackTrace();
        }
        while (finishedJobs < NumOfJobs) {
            List<Message> messages = sqsUtil.receiveMessages(config.workers2ManagerQ + random);
            if (messages.size() != 0) {
                for (Message msg : messages) {
                    System.out.println("Manager receieved message: " + msg.body());
                    temp = new Gson().fromJson(msg.body(), sqsMessage.class);

                    try {
                        assert fileWrite != null;
                        fileWrite.write(temp.body);
                        fileWrite.write("\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    finishedJobs++;
                    sqsUtil.deleteMessage(msg, config.workers2ManagerQ + random);
                }
            }

            if (messages.size() == 0) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        try {
            fileWrite.close();
        } catch (Exception e) {
            System.out.println("Error: couldn't close FileWriter fileWrite");
            e.printStackTrace();
        }

        System.out.println("File sent to bucket: " + message.body);
        s3Utils.putObject(Paths.get("WorkersOutput.txt"), "WorkersOutput.txt", message.body);
        WorkersOutputFile.delete();
        System.out.println("Manager finished a task");

        sqsMessage mess = new sqsMessage("NO_REPLY_NEEDED", "finished", false);
        sqsUtil.sendMessage(mess, sqsUtil.getQueueUrl(message.sqsReplyQ));
        sqsUtil.deleteQueue(config.workers2ManagerQ + random);
    }


    private synchronized static void MoreWorkers(int numToAdd) {
        if (numToAdd > MaxWorkers) {
            if ((MaxWorkers - CurrWorkers) > 0) {
                ec2Utils.startEc2s("Worker", bashCommand.initEmployee, (MaxWorkers - CurrWorkers));
                CurrWorkers = MaxWorkers;
            }
        } else if (numToAdd > CurrWorkers) {
            ec2Utils.startEc2s("Worker", bashCommand.initEmployee, numToAdd - CurrWorkers);
            CurrWorkers = numToAdd;
        }
        if (CurrWorkers >= MaxWorkers)
            return;
    }
}
