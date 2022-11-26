package Utils;

// imports
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class s3Utils {
    public static final S3Client s3 = S3Client.builder().region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(auth.getCredentials())).build();

    public static void createBucket(String name){
        s3.createBucket(CreateBucketRequest.builder().bucket(name).createBucketConfiguration(CreateBucketConfiguration.builder().build()).build());
        System.out.println("bucket has been created with the name: " + name);
    }

    public static void deleteBucket(String bucket) {
        deleteBucketObjects(bucket);
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
        s3.deleteBucket(deleteBucketRequest);
    }

    public static void deleteBucketObjects(String bucketName) {

        try {
            ListObjectsRequest req = ListObjectsRequest.builder().bucket(bucketName).build();
            ListObjectsResponse res = s3.listObjects(req);
            List<S3Object> objects = res.contents();

            for (S3Object x : objects) {
                deleteBucketObject(bucketName, x.key());
            }

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
    }

    public static void deleteBucketObject(String bucket, String obj) {

        ArrayList<ObjectIdentifier> lstOfObjects = new ArrayList<ObjectIdentifier>();
        lstOfObjects.add(ObjectIdentifier.builder().key(obj).build());

        try {
            DeleteObjectsRequest req = DeleteObjectsRequest.builder().bucket(bucket)
                    .delete(Delete.builder().objects(lstOfObjects).build()).build();
            s3.deleteObjects(req);
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
    }

    public static boolean checkIfBucketExistsAndHasAccessToIt(String bucket) {
        HeadBucketRequest req = HeadBucketRequest.builder().bucket(bucket).build();

        try {
            s3.headBucket(req);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    public static void putObject(String data, String key, String bucketName) {
        s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).build(),
                RequestBody.fromBytes(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static void putObjectPublic(String data, String key, String bucketName) {
        s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).acl(ObjectCannedACL.PUBLIC_READ).build(),
                RequestBody.fromBytes(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static void putObjectPublic(Path path, String key, String bucketName) {
        s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).acl(ObjectCannedACL.PUBLIC_READ).build(), RequestBody.fromFile(path));
    }

    public static void putObject(Path path, String key, String bucketName) {
        s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).build(), RequestBody.fromFile(path));
    }

    public static String getObject(String key, String bucketName) {
        BufferedReader reader;
        ResponseInputStream<GetObjectResponse> s3Obj = s3
                .getObject(GetObjectRequest.builder().key(key).bucket(bucketName).build());
        reader = new BufferedReader(new InputStreamReader(s3Obj));
        String line;
        StringBuilder obj = new StringBuilder();
        try {

            while ((line = reader.readLine()) != null) {
                obj.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return obj.toString();
    }

    public static boolean doesObjectExist(String bucket, String key) {
        ListObjectsResponse resp = s3.listObjects(ListObjectsRequest.builder().bucket(bucket).build());
        for(S3Object obj : resp.contents())
            if(obj.key().equalsIgnoreCase(key))
                return true;
        return false;

    }

    public static ResponseInputStream getObjectS3(String key, String bucketName) {
        return s3.getObject(GetObjectRequest.builder().key(key).bucket(bucketName).build());
    }

    public static void deleteObject(String key, String bucketName) {
        s3.deleteObject(DeleteObjectRequest.builder().key(key).bucket(bucketName).build());
    }
}
