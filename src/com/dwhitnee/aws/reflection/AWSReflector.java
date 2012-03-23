package com.dwhitnee.aws.reflection;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;

import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableResult;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;

import com.google.gson.Gson;


public class AWSReflector {

    static AmazonDynamoDBClient dynamoDB;

    public static void main( String[] args ) throws Exception {
        init();

        System.out.println("Hello World");

        System.out.println("Output: " + callDynDynamo());
    }


    static void init() throws Exception {

        AWSCredentials credentials = new PropertiesCredentials(
            ClassLoader.getSystemClassLoader().
            getResourceAsStream("AwsCredentials.properties"));

        dynamoDB = new AmazonDynamoDBClient(credentials);
    }



    static void callDynamo() {
        Gson gson = new Gson();

        DescribeTableRequest req =
            new DescribeTableRequest().withTableName("Customers");

        DescribeTableResult resp = dynamoDB.describeTable( req );

        TableDescription table = resp.getTable();

        System.out.println("Request: " + gson.toJson( req ) );
        System.out.println("Response: " + gson.toJson( resp ) );

        // System.out.println("Table Description: " + table );
        // System.out.println("Table JSON: " + gson.toJson( table ));


        // MyType target2 = gson.fromJson(json, MyType.class);
    }


    //----------------------------------------------------------------------
    static String callDynDynamo() {

        Gson gson = new Gson();

        String json = "{\"tableName\":\"Customers\"}";

        DescribeTableRequest req =
            gson.fromJson( json, DescribeTableRequest.class);

        DescribeTableResult resp = dynamoDB.describeTable( req );

        TableDescription table = resp.getTable();

        return gson.toJson( resp );

        // MyType target2 = gson.fromJson(json, MyType.class);
    }


}
