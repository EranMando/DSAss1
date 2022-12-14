package Manager;

import Local.Local;
import Utils.auth;
import Worker.Worker;

import java.io.File;

public class main{
    public static void main (String[] args) throws Exception{
        if (args == null || args.length == 0){
            System.out.println("Basic Usage: inputFileName outputFileName workerToTaskRatio [terminate]");
            return;
        }

        //we use getProperty user.home for this code so it can run on EC2 manager/worker instance AND on personal PC
        String path = System.getProperty("user.home") + File.separator + ".aws" + File.separator + "credentials.txt";

        //parse the AWS CLI credentials from the credentials file we created in our home directory
        auth.loadCredentials(path);

        //java -jar dps1-1.0-SNAPSHOT.jar Worker
        if (args[0].equals("Worker"))
            Worker.main(new String[]{});

            //java -jar dps-1.0-SNAPSHOT.jar input-sample.txt output.html 1 terminate
        else if (args.length == 4)
            Local.main(new String []{args[0], args[1], args[2], args[3]});
            //java -jar dps1-1.0-SNAPSHOT.jar input-sample.txt output.html 1
        else if (args.length == 3)
            Local.main(new String []{args[0], args[1], args[2]});

            //java -jar dps1-1.0-SNAPSHOT.jar Manager
        else if (args[0].equals("Manager"))
            Manager.main(new String[]{});
        else
            System.out.println("there is no case that matches ur input, please check Manager/main.java");
    }
}
