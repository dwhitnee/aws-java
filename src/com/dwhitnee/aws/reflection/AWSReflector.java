package com.dwhitnee.aws.reflection;

import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;

import java.lang.Throwable;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableResult;

import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;


// All clients
import com.amazonaws.AmazonWebServiceClient;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.importexport.AmazonImportExportClient;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQSClient;



/**
 * Text/JSON interface to the AWS Java SDK.
 *
 * Handles
 *  bad service name
 *  bad function name
 *  no connection
 *  IAM no permissions
 */

public class AWSReflector {

    public static void main( String[] argv ) throws Exception {
        AWSReflector foo = new AWSReflector();

        System.out.println("Hello World");

        Map<String,String> args = new HashMap<String,String>();
        args.put("tableName", "Customers");

        System.out.println("Output: " +
                           foo.callDynamic("dynamodb", "describeTable", args ));
    }

    //----------------------------------------------------------------------
    Map<String,AmazonWebServiceClient> _services;
    AWSCredentials _credentials;
    Gson _gson;

    //----------------------------------------------------------------------
    public AWSReflector() throws Exception
    {
        init();
        _gson = new Gson();
    }

    //----------------------------------------------------------------------
    void init() throws Exception
    {
        _credentials = new PropertiesCredentials(
            ClassLoader.getSystemClassLoader().
            getResourceAsStream("AwsCredentials.properties"));

        _services = new HashMap<String,AmazonWebServiceClient>();
    }

    //----------------------------------------------------------------------
    protected enum AWSService {
        autoscaling, cloudformation, cloudfront, cloudwatch, dynamodb, ec2,
        elasticache, elasticbeanstalk, elasticloadbalancing, elasticmapreduce,
        identitymanagement, importexport, rds, route53, s3, securitytoken,
        simpledb, simpleemail, simpleworkflow, sns, sqs
    };

    //----------------------------------------------------------------------
    // Turns "ec2" into AmazonEC2Client.  Will cache, but cache (or this whole
    // object) should be thrown out if credentials change.
    //----------------------------------------------------------------------
    protected AmazonWebServiceClient getClient( String serviceName )
    {
        AmazonWebServiceClient outService = _services.get( serviceName );

        if (outService == null) {

            AWSService service = null;
            try {
                service = AWSService.valueOf( serviceName );
            }
            catch (Exception ex) {
                throw new RuntimeException(
                    "Service "+ serviceName +" not supported");
            }

            switch (service)
            {
                case autoscaling:
                    outService = new AmazonAutoScalingClient( _credentials );
                    break;
                case cloudformation:
                    outService = new AmazonCloudFormationClient( _credentials );
                    break;
                case cloudfront:
                    outService = new AmazonCloudFrontClient( _credentials );
                    break;
                case cloudwatch:
                    outService = new AmazonCloudWatchClient( _credentials );
                    break;
                case dynamodb:
                    outService = new AmazonDynamoDBClient( _credentials );
                    break;
                case ec2:
                    outService = new AmazonEC2Client( _credentials );
                    break;
                case elasticache:
                    outService = new AmazonElastiCacheClient( _credentials );
                    break;
                case elasticbeanstalk:
                    outService = new AWSElasticBeanstalkClient( _credentials );
                    break;
                case elasticloadbalancing:
                    outService = new AmazonElasticLoadBalancingClient( _credentials );
                    break;
                case elasticmapreduce:
                    outService = new AmazonElasticMapReduceClient( _credentials );
                    break;
                case identitymanagement:
                    outService = new AmazonIdentityManagementClient( _credentials );
                    break;
                case importexport:
                    outService = new AmazonImportExportClient( _credentials );
                    break;
                case rds:
                    outService = new AmazonRDSClient( _credentials );
                    break;
                case route53:
                    outService = new AmazonRoute53Client( _credentials );
                    break;
                case s3:
                    outService = new AmazonS3Client( _credentials );
                    break;
                case simpledb:
                    outService = new AmazonSimpleDBClient( _credentials );
                    break;
                case simpleemail:
                    outService = new AmazonSimpleEmailServiceClient( _credentials );
                    break;
                case simpleworkflow:
                    outService = new AmazonSimpleWorkflowClient( _credentials );
                    break;
                case sns:
                    outService = new AmazonSNSClient( _credentials );
                    break;
                case sqs:
                    outService = new AmazonSQSClient( _credentials );
                    break;
                case securitytoken:
                    outService = new AWSSecurityTokenServiceClient( _credentials );
                    break;

                default:
                    // should never get here, enum should match above switch.
                    throw new RuntimeException(
                        "Service " + serviceName + " not supported");
            }
            _services.put( serviceName, outService );
        }

        return outService;
    }

    //----------------------------------------
    // For Jsonification
    //----------------------------------------
    class ErrorData {
        String error;
        int code;
        String message;

        public ErrorData( String e, int c, String m) {
            error = e;
            code = c;
            message = m;
        }
    };

    //----------------------------------------
    /**
     * Takes ("ec2", "describeInstances") and via reflection turns that into
     * AmazonEC2Client.desscribeInstances( DescribeInstancesRequest )
     *
     * @return Response object as json.
     */
    //----------------------------------------
    String callDynamic( String service,
                        String methodName,
                        Map<String,String> inArgs)
        throws Exception
    {
        String baseClassName = "com.amazonaws.services." + service + ".model.";
        String requestName = StringUtils.capitalize( methodName ) + "Request";
        String requestClassName = baseClassName + requestName;

        //  {"tableName":"Customers"}";
        String argsJson = _gson.toJson( inArgs );

        AmazonWebServiceClient client = null;
        Object request = null;
        Class requestClass = null;

        // if service name or function is bad, return a 400
        try {
            client = getClient( service );
        }
        catch (Exception ex) {
            ErrorData error = new ErrorData(
                "Bad Request", 400,
                "Unknown AWS Service: " + service);
            return _gson.toJson( error );
        }

        try {
            requestClass = Class.forName( requestClassName );
        }
        catch (Exception ex) {
            ErrorData error = new ErrorData(
                "Bad Request", 400,
                "Unknown "+ service +" action: "+ methodName +"");
            return _gson.toJson( error );
        }

        try {
            request = _gson.fromJson( argsJson, requestClass );

            // get method for, e.g., describeTable
            Method method = client.getClass().
                getMethod( methodName,
                           new Class[] { requestClass } );

            // invoke method, this could fail in many ways, try to handle them
            try {
                return _gson.toJson(
                    method.invoke( client, new Object[] { request }));
            }
            catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                // bad connection
                if (cause instanceof AmazonClientException) {
                    AmazonClientException ace = (AmazonClientException) cause;

                    return _gson.toJson(
                        new ErrorData("BadNetwork", 408, ace.getMessage()));
                }

                return _gson.toJson(
                    new ErrorData("oops", 1, ex.getMessage()));
            }
        }
        catch (AmazonServiceException ex) {
            ErrorData error = new ErrorData( ex.getErrorCode(),
                                             ex.getStatusCode(),
                                             ex.toString() );
            return _gson.toJson( error );
        }
        catch ( Exception ex) {
            ex.printStackTrace();
            ErrorData error = new ErrorData("oops", 500, "");
            return "{ 'error':'oops: " + ex + "'}";
        }
    }





    void callDynamo() {
        AmazonDynamoDBClient dynamoDB = new AmazonDynamoDBClient( _credentials );

        DescribeTableRequest req =
            new DescribeTableRequest().withTableName("Customers");

        DescribeTableResult resp = dynamoDB.describeTable( req );

        TableDescription table = resp.getTable();

        System.out.println("Request: " + _gson.toJson( req ) );
        System.out.println("Response: " + _gson.toJson( resp ) );
    }

}
