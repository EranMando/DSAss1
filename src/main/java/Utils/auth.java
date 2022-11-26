package Utils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

import java.io.BufferedReader;
import java.io.FileReader;

public class auth {
    public static String accessKey;
    public static String secretKey;
    public static String sessionToken;

    public static void loadCredentials(){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(config.credPath)); // path of credentials
            reader.readLine();
            String line = reader.readLine();
            accessKey = line.split("=", 2)[1];
            line = reader.readLine();
            secretKey = line.split("=", 2)[1];
            line = reader.readLine();
            if (line!=null)
                sessionToken = line.split("=", 2)[1];
            reader.close();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public static void loadCredentials(String path){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(path)); // path of credentials
            reader.readLine();
            String line = reader.readLine();
            accessKey = line.split("=", 2)[1];
            line = reader.readLine();
            secretKey = line.split("=", 2)[1];
            line = reader.readLine();
            if (line!=null)
                sessionToken = line.split("=", 2)[1];
            reader.close();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    static public AwsCredentials getCredentials()
    {
        if(accessKey == null)
            loadCredentials();
        if (sessionToken==null)
            return AwsBasicCredentials.create(accessKey, secretKey);
        else
            return AwsSessionCredentials.create(accessKey,secretKey,sessionToken);
    }
}
