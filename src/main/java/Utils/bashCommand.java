package Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class bashCommand {
    public static String initInstanceBash;
    public static String initManager;
    public static String initEmployee;


    public static String loadCredentials(){
        try {
            String tmp = System.getProperty("user.home") + File.separator + ".aws/credentials.txt";
            File credPath = new File(tmp);
            BufferedReader reader = new BufferedReader(new FileReader(credPath));
            StringBuilder ret = new StringBuilder();

            String line;
            while((line = reader.readLine()) != null) {
                ret.append(line).append("\n");
            }

            reader.close();
            return ret.toString();
        } catch (IOException var5) {
            System.out.println(var5);
            return null;
        }
    }


    static {
        initManager = "#!/bin/bash -x\n"
                //+ "sudo mkdir /root/.aws\n"
                //+ "echo \"" + loadCredentials() + "\" > /root/.aws/credentials" + "\n"
                //+ "AWS_CONFIG_FILE=\"~/.aws/credentials\""
                + "aws configure set aws_access_key_id " + auth.accessKey + "\n"
                + "aws configure set aws_secret_access_key " + auth.secretKey + "\n"
                + "aws configure set region us-east-1"
                + "mkdir ass1\n"
                + "aws s3 cp s3://"+config.s3Bucket+"/Manager.jar ass1/Manager.jar\n"
                + "yes | sudo yum install java-1.8.0\n"
                + "yes | sudo yum remove java-1.7.0-openjdk\n"
                + "sudo java -jar ass1/Manager.jar " + config.runManager + "\n";
    }


}
// aws s3 cp s3://ass1bucketdps/Manager.jar ass1/Manager.jar
