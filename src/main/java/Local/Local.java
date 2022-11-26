package Local;

import Utils.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;

public class Local {
    final static String local2MangerBucket = config.localToManager + ((int)(Math.random() * 1000000)) + "bucket" + ((int)(Math.random() * 100));
    final static String manager2LocalQ = config.managerToLocal + ((int)(Math.random() * 1000000)) + "sqs" + ((int)(Math.random() * 100));

    public static void main(String[] args){
        System.out.println("Local App Init --- ");
        String cwd = System.getProperty("user.dir");
        // args check
        if(args.length != 3 && args.length != 4){
            System.out.println("Error: wrong args numbers");
        }

        // input file
        File input_file = new File(args[0]);
        if(!input_file.exists()){
            System.out.println("input file provided does not exist");
            return;
        }
        // terminate flag
        boolean shouldTerminate = args.length == 4 && args[3].equalsIgnoreCase("terminate");


        // argument checking
//        String inputFilePath = args[0];
//        String outputFilePath = args[1];
//        int amountWorkers = Integer.parseInt(args[2]);

//
//        File file = new File("./artifacts/"+inputFilePath);
//        if(!file.exists()){
//            System.out.println("file does not exist");
//            return;
//        }

        // create bucket and upload input file to it
        auth.loadCredentials();
        if(!s3Utils.checkIfBucketExistsAndHasAccessToIt(config.s3Bucket))
            s3Utils.createBucket(config.s3Bucket);

        //We upload the jar for the workers and manager to know what to run.
        //s3Utils.putObjectPublic(Paths.get(cwd+"/dps1-1.0-SNAPSHOT.jar"),"JAR",config.s3Bucket);

        // start the manager if exist
        ec2Utils.startEc2IfNotExist("Manager",bashCommand.initManager);

        // create sqsQ local -> manager
        sqsUtil.createSqsQueue(config.localToManager);

        // local->manager bucket // results bucket
        if(!s3Utils.checkIfBucketExistsAndHasAccessToIt(local2MangerBucket)){
            s3Utils.createBucket(local2MangerBucket);
        }
        if (!s3Utils.checkIfBucketExistsAndHasAccessToIt(config.OCRResults)){
            s3Utils.createBucket(config.OCRResults);
        }

        // create sqsQ manager -> local
        sqsUtil.createSqsQueue(manager2LocalQ);

        // uploading input file
        s3Utils.putObject(Paths.get(cwd+ File.separator +args[0]),"InputFile",local2MangerBucket);
        System.out.println("input file uploaded");

        // create message from local to manager and send the input file with the termination flag
        sqsMessage mess = new sqsMessage(manager2LocalQ,local2MangerBucket,shouldTerminate);
        mess.workerImageRatio = Integer.parseInt(args[2]);
        sqsUtil.sendMessage(mess,sqsUtil.getQueueUrl(config.localToManager));


        // dealing with the output
        ResponseInputStream workerOutput = null;
        while(true){
            try {
                List<Message> messages = sqsUtil.receiveMessages(manager2LocalQ);
                if (messages.size() > 0) {
                    System.out.println("Message content: " + messages.get(0).body());

                    sqsUtil.deleteMessage(messages.get(0), manager2LocalQ);
                    workerOutput = s3Utils.getObjectS3("WorkersOutput.txt", local2MangerBucket);

                    // preparing the html file
                    BufferedReader reader = new BufferedReader(new InputStreamReader(workerOutput));
                    FileWriter fw = new FileWriter(cwd + File.separator + args[1]);
                    Writer writer = new BufferedWriter(fw);

                    // html temp
                    writer.write("<!DOCTYPE html>");
                    writer.write("<html>");
                    writer.write("<body>");
                    // parse our output message

                    String[] splits = new String[0];
                    String str = "";

                    String Line;
                    while((Line = reader.readLine()) != null) {
                        splits = Line.split(";");
                        str = "Image url: " + splits[0] + "\n" + "text result:  " + splits[1];
                        str+=  "<p>\n" +
                                "<img src="+splits[0]+"><br>"+
                                splits[1]+"<br></p>\n";
                        writer.write(str);
                        writer.write(10);
                    }

                    // html end
                    writer.write("</body>");
                    writer.write("</html>");
                    writer.close();

                    // deleting messages from Qs
                    sqsUtil.deleteQueue(manager2LocalQ);
                    s3Utils.deleteBucket(local2MangerBucket);
                }
            }catch (Exception e){System.out.println(e.getMessage());}
        }


    }
}
