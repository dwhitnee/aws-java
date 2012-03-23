package com.dwhitnee.aws.reflection;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;

import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;


public class AWSReflector {

    static AmazonDynamoDBClient dynamoDB;

    public static void main( String[] args ) throws Exception {
        init();

        System.out.println("Hello World");

        DescribeTableRequest describeTableRequest =
            new DescribeTableRequest().withTableName("Customers");

        TableDescription tableDescription =
            dynamoDB.describeTable( describeTableRequest ).getTable();

        System.out.println("Table Description: " + tableDescription);
    }


    private static void init() throws Exception {

        AWSCredentials credentials = new PropertiesCredentials(
            ClassLoader.getSystemClassLoader().
            getResourceAsStream("AwsCredentials.properties"));


//         AWSCredentials credentials = new PropertiesCredentials(
//             AWSReflector.class.getResourceAsStream(
//                 "AwsCredentials.properties"));

        dynamoDB = new AmazonDynamoDBClient(credentials);
    }



}
