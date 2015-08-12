package com.dwhitnee.aws.sdk;

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

import com.amazonaws.AmazonWebServiceClient;



/**
 * Text/JSON interface to the AWS Java SDK.
 *
 * Handles
 *  bad service name
 *  bad function name
 *  no connection
 *  IAM no permissions
 */

public class AWSInvoker {

    public static void main( String[] argv ) throws Exception {

        AWSCredentials creds = new PropertiesCredentials(
            ClassLoader.getSystemClassLoader().
            getResourceAsStream("AwsCredentials.properties"));

        AWSReflector aws = new AWSReflector( creds );

        System.out.println("Hello World");

        Map<String,String> args = new HashMap<String,String>();

        System.out.println( aws.call("/POOP/describeTable", args ));
        System.out.println( aws.call("/dynamodb/POOPdescribeTable", args ));
        System.out.println( aws.call("/dynamodb/describeTable", args ));
        args.put("tableName", "ZZCustomers");
        System.out.println( aws.call("/dynamodb/describeTable", args ));
        args.put("tableName", "Customers");
        System.out.println( aws.call("/dynamodb/describeTable", args ));

        System.out.println( aws.call("/ec2/describeAvailabilityZones", null ));


        Map<String,String[]> sgs = new HashMap<String,String[]>();
        sgs.put("groupNames", new String[] { "Minecraft" });
        System.out.println( aws.call("ec2", "describeSecurityGroups",
                                     new Gson().toJson( sgs )));
    }

    //----------------------------------------------------------------------

    AWSCredentials                      _credentials;
    Map<String,AmazonWebServiceClient>  _services;
    Gson _gson;

    protected static final Map<String, String>   _clientNames =
        Collections.unmodifiableMap(new HashMap<String, String>() {{
                    put("autoscaling", "AmazonAutoScaling");
                    put("cloudformation", "AmazonCloudFormation");
                    put("cloudfront", "AmazonCloudFront");
                    put("cloudwatch", "AmazonCloudWatch");
                    put("dynamodb",   "AmazonDynamoDB");
                    put("ec2",        "AmazonEC2");
                    put("elasticache","AmazonElastiCache");
                    put("elasticbeanstalk",    "AWSElasticBeanstalk");
                    put("elasticloadbalancing","AmazonElasticLoadBalancing");
                    put("elasticmapreduce",    "AmazonElasticMapReduce");
                    put("identitymanagement",  "AmazonIdentityManagement");
                    put("importexport", "AmazonImportExport");
                    put("rds",     "AmazonRDS");
                    put("route53", "AmazonRoute53");
                    put("s3",      "AmazonS3");
                    put("securitytoken", "AWSSecurityTokenService");
                    put("simpledb",      "AmazonSimpleDB");
                    put("simpleemail",   "AmazonSimpleEmailService");
                    put("simpleworkflow", "AmazonSimpleWorkflow");
                    put("sns",   "AmazonSNS");
                    put("sqs",   "AmazonSQS");
                }});



    //----------------------------------------------------------------------
    public AWSReflector( AWSCredentials inCredentials) throws Exception
    {
        _credentials = inCredentials;
        _services = new HashMap<String,AmazonWebServiceClient>();
        _gson = new Gson();
    }

    //----------------------------------------------------------------------
    // convert "/ec2/describeInstances" to real call
    //----------------------------------------------------------------------
    public String call( String path,
                        Map<String,String> args )
    {
        String[] request = path.split("/");
        // assert len > 2  FIXME
        return call( request[1], request[2], args );
    }

    //----------------------------------------------------------------------
    // convert "/sdk/ec2/describeInstances" to real call
    //----------------------------------------------------------------------
/*
    public String call( HttpRequestServlet servlet ) {

        String[] request = servlet.getServletPath().split("/");
        String jsonArgs = servlet.getParamter("args");

        // assert request.len > 3
        return call( request[2], request[3], jsonArgs );
    }
*/
    //----------------------------------------------------------------------
    String call( String service,
                 String methodName,
                 Map<String,String> args)
    {
        if (args == null)
            return call( service, methodName, "{}"); // to avoid actual "null"
        else
            return call( service, methodName, _gson.toJson( args ));
    }

    //----------------------------------------
    /**
     * Takes ("ec2", "describeInstances") and via reflection turns that into
     * AmazonEC2Client.desscribeInstances( DescribeInstancesRequest )
     *
     * @param inArgs expects parameters in the form the SDK wants.
     * @return Response object as json.
     */
    //----------------------------------------
    String call( String service,
                 String methodName,
                 String jsonArgs)
    {
        String baseClassName = "com.amazonaws.services." + service + ".model.";
        String requestName = StringUtils.capitalize( methodName ) + "Request";
        String requestClassName = baseClassName + requestName;

        // args are Ex: {"tableName":"Customers"}";

        AmazonWebServiceClient client = null;
        Object request = null;
        Class<?> requestClass = null;

        try {
            client = getClient( service );
        }
        catch (Exception ex) {
            return errorJSON("BadRequest", 400,
                             "Unknown AWS Service: " + service);
        }
        try {
            requestClass = Class.forName( requestClassName );
        }
        catch (Exception ex) {
            return errorJSON("Bad Request", 400,
                             "Unknown "+ service +" action: "+ methodName +"");
        }

        try {
            // convert request args map to request object

            if (jsonArgs == null)
                request = requestClass.newInstance();
            else
                request = _gson.fromJson( jsonArgs, requestClass );

            if (request == null)
                return errorJSON("Bad Request", 400, "Invalid arguments to " +
                                 service+"."+methodName+": " + jsonArgs);

            // e.g., describeIsntances()
            Method method = client.getClass().
                getMethod( methodName, new Class[] { requestClass } );

            // invoke method, this could fail in many ways, try to handle them
            try {
                return _gson.toJson(
                    method.invoke( client, new Object[] { request }));
            }
            catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();

                if (cause instanceof AmazonServiceException) {
                    AmazonServiceException ase = (AmazonServiceException) cause;
                    return errorJSON( ase.getErrorCode(), ase.getStatusCode(),
                                      service+"."+methodName+": " +
                                      ase.getMessage() );

                } else if (cause instanceof AmazonClientException) {
                    ex.printStackTrace();
                    // no network...?
                    return errorJSON("BadNetwork", 408, cause.getMessage());
                } else {
                    // permissions? service? who knows?
                    return errorJSON("oops", 1, ex.getMessage());
                }
            }
        }
        catch ( Exception ex) {
            ex.printStackTrace();
            return errorJSON("oops", 500, "");
        }
    }



    //----------------------------------------------------------------------
    // Error Handling
    //----------------------------------------------------------------------

    class ErrorData {
        String code;  // brief error
        int status;   // HTTP status
        String message;  // details

        public ErrorData( String c, int s, String m) {
            code = c;
            status = s;
            message = m;
        }
    };

    class Errors {
        ErrorData[] error;
        public Errors( ErrorData e) { error = new ErrorData[] { e }; }
    }

    //----------------------------------------------------------------------
    // error: [{
    //   code: "Bad Request"
    //   status: 403
    //   message: "lorem ipsum blah"
    // }]

    String errorJSON( String error, int statusCode, String message) {
        return _gson.toJson(
            new Errors( new ErrorData( error, statusCode, message )));
    }




    //----------------------------------------------------------------------
    // Declaration of all services
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

}


