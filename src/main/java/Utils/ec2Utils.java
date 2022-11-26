package Utils;

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Base64;
import java.util.Iterator;
import java.util.List;

public class ec2Utils {
    public static Ec2Client ec2 = Ec2Client.builder().region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(auth.getCredentials()))
            .build();

    public static void startEc2s(String name, String init, int amount) {
        System.out.println("Creating (" + amount + ") Ec2 Instances: " + name);

        //create & run the instance
        List<Instance> instances = ec2.runInstances(RunInstancesRequest.builder()
                .maxCount(amount)
                .minCount(amount)
                .imageId("ami-0b0dcb5067f052a63")
                .userData(Base64.getEncoder().encodeToString(init.getBytes()))
                .instanceType(InstanceType.T2_MICRO)
                .build()).instances();

        //tag the instance properly with the name
        Tag tag = Tag.builder().key("Job").value(name).build();
        for (Instance instance_ : instances) {
            if (!instance_.state().name().toString().equals("terminated")) {
                CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                        .resources(instance_.instanceId())
                        .tags(tag)
                        .build();
                ec2.createTags(tagRequest);
            }
        }
    }

    public static void startEc2IfNotExist(String name, String init) {
        String nextToken = null;

        do {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder().nextToken(nextToken)
                    .build();
            DescribeInstancesResponse response = ec2.describeInstances(request);
            //if no reservations are ever made, then no EC2 instances ever existed...
            if (response.reservations().size() == 0) {
                startEc2s(name, init, 1);
                return;
            }
            for (Reservation reservation : response.reservations()) {
                //if no reservation are made for instances, then no EC2 instances ever existed...
                if (reservation.instances().size() == 0) {
                    startEc2s(name, init, 1);
                    return;
                }
                for (Instance instance : reservation.instances()) {
                    if (reservation.instances().size() == 0) {
                        startEc2s(name, init, 1);
                        return;
                    }
                    if (instance.tags().size() != 0 && (instance.state().name().toString().equalsIgnoreCase("running") || instance.state().name().toString().equalsIgnoreCase("pending")) && instance.tags().get(0).value().equals(name)) {
                        System.out.println("Manager already running!");
                        return;
                    }
                }
            }
            nextToken = response.nextToken();
        } while (nextToken != null);
        startEc2s(name, init, 1);
    }

    public static void terminateAll() {
        String nextToken = null;

        try {
            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().nextToken(nextToken)
                        .build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        if (!instance.state().name().toString().equals("terminated")) {
                            ec2.terminateInstances(TerminateInstancesRequest.builder().instanceIds(instance.instanceId()).build());
                            System.out.println("TERMINATED INSTANCE:" + instance.tags().get(0).value());
                        }
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            return;
        }
    }

    public static void terminateAll(String name) {
        String nextToken = null;

        try {
            do {
                DescribeInstancesRequest request = (DescribeInstancesRequest)DescribeInstancesRequest.builder().nextToken(nextToken).build();
                DescribeInstancesResponse response = ec2.describeInstances(request);
                Iterator var4 = response.reservations().iterator();

                while(var4.hasNext()) {
                    Reservation reservation = (Reservation)var4.next();
                    Iterator var6 = reservation.instances().iterator();

                    while(var6.hasNext()) {
                        Instance instance = (Instance)var6.next();
                        if (instance.tags().size() != 0 && !instance.state().name().toString().equals("terminated") && ((Tag)instance.tags().get(0)).value().equals(name)) {
                            ec2.terminateInstances((TerminateInstancesRequest)TerminateInstancesRequest.builder().instanceIds(new String[]{instance.instanceId()}).build());
                            System.out.println("TERMINATED INSTANCE:" + ((Tag)instance.tags().get(0)).value());
                        }
                    }
                }

                nextToken = response.nextToken();
            } while(nextToken != null);

        } catch (Ec2Exception var8) {
            System.err.println(var8.awsErrorDetails().errorMessage());
        }
    }
}
