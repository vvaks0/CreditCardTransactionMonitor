package com.hortonworks.iot.financial;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.hortonworks.iot.financial.rest.CreditCardSimulatorService;
import com.hortonworks.iot.financial.types.TransactionSimulator;

import net.sf.ehcache.CacheManager;

public class CreditCardTransactionSimulator {

	 // Base URI the Grizzly HTTP server will listen on
    public static String ipaddress;
    public static String port;
	public static String targetIP;
	public static String targetPort;
	public static HttpServer startServer(String simType, String deviceId) {
    	//Map<String,String> deviceDetailsMap = new HashMap<String, String>();
    	Map<String,String> deviceNetworkInfoMap = new HashMap<String, String>();
    	ResourceConfig config = null;
    	URI baseUri = null;
    	deviceNetworkInfoMap = getNetworkInfo(deviceId, simType);
    	baseUri = UriBuilder.fromUri("http://"+ deviceNetworkInfoMap.get("ipaddress") + "/server/").port(Integer.parseInt(deviceNetworkInfoMap.get("port"))).build();
    	//deviceDetailsMap = getSimulationDetails(simType, deviceId);
		//baseUri = UriBuilder.fromUri("http://" + deviceDetailsMap.get("ipaddress") + "/server/").port(Integer.parseInt(deviceDetailsMap.get("port"))).build();
	
    	if(simType.equalsIgnoreCase("Customer")){
    		config = new ResourceConfig(CreditCardSimulatorService.class);
    	}
    	else{
    		System.exit(1);
    	}
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
		return server;
    }
    public static Map<String,String> getNetworkInfo(String deviceId, String simType){
    	Map<String,String> deviceNetworkInfoMap = new HashMap<String, String>();
        String ipaddress;
    	String hostname;
        String ipScratch[];
        try {
            ipScratch = InetAddress.getLocalHost().toString().replace("/", ":").split(":"); 
            ipaddress = InetAddress.getLocalHost().getHostAddress();
            hostname = InetAddress.getLocalHost().getHostName();
            deviceNetworkInfoMap.put("ipaddress", ipaddress);
            deviceNetworkInfoMap.put("hostname", hostname);
            System.out.println("Current IP address : " + ipaddress);
            System.out.println("Current Hostname : " + hostname);
            System.out.println("Target IP address : " + targetIP);
            System.out.println("Target Port : " + targetPort);  
        } catch (UnknownHostException e) {
 
            e.printStackTrace();
        }
        
        switch(simType + " " + deviceId){
		case "Customer 1000":
			deviceNetworkInfoMap.put("port", "8084");
			break;
		case "Customer 2000":
			deviceNetworkInfoMap.put("port", "8085");
			break;
		case "Customer 3000":
			deviceNetworkInfoMap.put("port", "8086");
			break;
		case "Customer 4000":
			deviceNetworkInfoMap.put("port", "8087");
			break;
		case "Customer 5000":
			deviceNetworkInfoMap.put("port", "8088");
			break;
		case "Customer 6000":
			deviceNetworkInfoMap.put("port", "8089");
			break;	
		default:
			System.out.println("There is no record of " + simType + " " + deviceId + ". Cannot start device simulation");
			System.exit(1);
			break;
		}
        
        return deviceNetworkInfoMap;
    }
    
	public static void main(String[] args) throws IOException, ParseException {
		Thread customerThread;
		String simType = args[0];
		String customerId = args[1];
		String mode = args[2];
		if(args.length > 3){	
			targetIP = args[3];
		}else{
			targetIP = "sandbox.hortonworks.com";
		}
		if(args.length > 4){	
			targetPort = args[4];
		}else{
			targetPort = "8082";
		}
		System.out.println("Starting Cache...");
		CacheManager.create();
		CacheManager.getInstance().addCache("CustomerActionRequest");
		
	  if(simType.equalsIgnoreCase("Customer")){			
			System.out.println("Starting Webservice...");
			final HttpServer server = startServer(simType, customerId);
			server.start();
			System.out.println("Starting Customer Route");
			Map networkInfo = getNetworkInfo(customerId, simType);
			ipaddress =  (String)networkInfo.get("ipaddress");
			port =  (String)networkInfo.get("port");
			TransactionSimulator tech = new TransactionSimulator(customerId, targetIP, targetPort, ipaddress, port, mode);
            customerThread = new Thread(tech);
            customerThread.setName("Customer: " + customerId);
            customerThread.start();
        }
    }
}
