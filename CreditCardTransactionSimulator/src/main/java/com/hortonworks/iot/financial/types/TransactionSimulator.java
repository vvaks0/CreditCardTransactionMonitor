package com.hortonworks.iot.financial.types;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.appengine.api.search.GeoPoint;
import com.hortonworks.iot.financial.events.CreditCardTransaction;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class TransactionSimulator implements Runnable {
    private String customerId;
    private String ipaddress;
    private String port;
    private String accountNumber;
    private String accountType;
    private String expMonth;
    private String expYear;
    private String mode;
    private String targetIP;
    private String targetPort;
    private long currentTimeEpoch;
    private long simulationEpochStart = Long.valueOf("1362096000000"); //Fri, 01 Mar 2013 00:00:00 GMT
    private Map<String,String> transactionAmountByVendor = new HashMap<String,String>();
    private List<List<Merchant>> tripList = new ArrayList<List<Merchant>>();
    private List<Merchant> currentTrip = new ArrayList<Merchant>();
    private List<String> transactionList = new ArrayList<String>();
    
    Random random = new Random();
    
    public TransactionSimulator(String customerId, String targetIP, String targetPort, String ipaddress, String port, String mode) throws ParseException {
        initialize(customerId, targetIP, targetPort, ipaddress, port, mode);
    }
    
    public void initialize(String customerId, String targetIP, String targetPort, String ipaddress, String port, String mode) throws ParseException{
        this.customerId = customerId;
        this.ipaddress = ipaddress;
        this.targetIP = targetIP;
        this.targetPort = targetPort;
        this.port = port;
        this.mode = mode;
        this.currentTimeEpoch = getCurrentDateEpochStart();
        
        //Need to tie this data structure to the merchant array in the tranaction history generator
        transactionAmountByVendor.put("grocery_or_supermarket","25-150");
        transactionAmountByVendor.put("restaurant","25-150");
        transactionAmountByVendor.put("bar","25-50");
        transactionAmountByVendor.put("clothing_store","50-300");
        transactionAmountByVendor.put("health_beauty","25-150");
        transactionAmountByVendor.put("electronics_store","50-150");
        transactionAmountByVendor.put("gas_station","25-50");
        transactionAmountByVendor.put("entertainment","30-50");
        transactionAmountByVendor.put("convenience_store","10-25");
        
        
        switch(customerId){
        	case "1000":
        		setAccountNumber("19123");
        		setAccountType("VISA");
        		Merchant merchant = new Merchant("1000", "grocery_or_supermarket", new GeoPoint(39.915854, -75.011355)); //Whole Foods, Ellisburgh Circle, Cherry Hill NJ
        		currentTrip.add(merchant); 
        		merchant = new Merchant("1001","convenience_store", new GeoPoint(39.914441, -75.011329)); //Bank of America, Ellisburgh Circle, Cherry Hill NJ)
        		currentTrip.add(merchant);
        		merchant = new Merchant("1002","restaurant", new GeoPoint(39.924953, -75.035022)); //Zinburger, Haddonfield Rd, Cherry Hill NJ
        		currentTrip.add(merchant);
        		merchant = new Merchant("1003","clothing_store", new GeoPoint(39.940682, -75.027550)); //Nordstrom, Cherry Hill Mall, Cherry Hill NJ
        		currentTrip.add(merchant);
        		merchant = new Merchant("1004","health_beauty", new GeoPoint(39.941060, -75.025777)); //Bare Minerals, Cherry Hill Mall, Cherry Hill NJ 
        		currentTrip.add(merchant);
        		merchant = new Merchant("1005","electronics_store", new GeoPoint(39.940786, -75.024436)); //Apple Store, Cherry Hill Mall, Cherry Hill NJ
        		currentTrip.add(merchant);
        		merchant = new Merchant("1006","gas_station", new GeoPoint(39.934361, -75.068378)); //Wawa Gas Station, Route 70, Cherry Hill NJ
        		currentTrip.add(merchant);
        		merchant = new Merchant("1007","entertainment", new GeoPoint(39.948443, -75.155545)); //Walnut St. Theater, 9th&Walnut Philadelphia PA
        		currentTrip.add(merchant);
        		merchant = new Merchant("1008","restaurant", new GeoPoint(39.949564, -75.143911)); //Cuba Libre, 2nd St&Market, Philadelphia PA 
        		currentTrip.add(merchant);
        		merchant = new Merchant("1009","convenience_store",new GeoPoint(39.906331, -74.974442)); //Wawa Mart, Marlton Pike&Marlkress, Cherry Hill NJ
        		currentTrip.add(merchant); 
        	break;
        	case "2000":
        		setAccountNumber("19103");
        	break;
        	case "3000":
        		setAccountNumber("19147");
        	break;
        	
        }
        tripList.add(currentTrip);
        System.out.println("***** ");
    }

    public void run() {
    	Merchant currentMerchant;
    	CacheManager cacheManager = CacheManager.getInstance();
		Cache cache = cacheManager.getCache("CustomerActionRequest");
		
		/*
		try {
			simulateTransactionHistory();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		
		
        Iterator<Merchant> currentMerchantIterator;
        for(int i=0; i<tripList.size(); i++) {
            currentTrip = tripList.get(i);
            currentMerchantIterator = currentTrip.iterator();
            System.out.println(currentMerchantIterator.hasNext());
            while(currentMerchantIterator.hasNext()){
            	//If the Customer Action Request Cache conatins a Tech Destination
            	if(cache.get(customerId) != null){
            		//newTechnicianDestination = (TechnicianDestination)((Element) cache.get(customerId)).getObjectValue();
            		//System.out.println("Recieved Request for Action :" + newTechnicianDestination);
            		//addTechnicianDestination(newTechnicianDestination);
            	}
            	
                currentMerchant = currentMerchantIterator.next();
            	Integer incident = random.nextInt(11-1) + 1;
            	
            	System.out.println("Transaction Integrity Dice Roll: " + incident);
            			
            	if(incident > 5)
            		generateFraudulentTransaction(currentMerchant);
            	else
            		generateLegitimateTransaction(currentMerchant);
            	
            	currentTimeEpoch+=3600000;
                try {
                	Thread.sleep(5000);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }   
            }
        }
        run();
    }
    
    public void generateLegitimateTransaction(Merchant merchant){
    	CreditCardTransaction transaction = new CreditCardTransaction();
    	System.out.println(transactionAmountByVendor.size());
    	System.out.println(merchant.getMerchantType());
    	System.out.println(transactionAmountByVendor.get(merchant.getMerchantType()));
    	
    	Integer lowerBound = Integer.valueOf(transactionAmountByVendor.get(merchant.getMerchantType()).split("-")[0]);
    	Integer upperBound = Integer.valueOf(transactionAmountByVendor.get(merchant.getMerchantType()).split("-")[1]);
    	System.out.println("Generating Legitimate Transaction");
    	
    	transaction.setAccountNumber(accountNumber);
    	transaction.setAccountType(accountType);
    	transaction.setMerchantId(merchant.getMerchantId());
    	transaction.setMerchantType(merchant.getMerchantType());
    	transaction.setTransactionId(merchant.getMerchantId()+String.valueOf(currentTimeEpoch));
    	transaction.setCurrency("USD");
    	transaction.setIsCardPresent("true");
    	transaction.setAmount(String.valueOf(random.nextInt(upperBound-lowerBound) + lowerBound));
    	transaction.setIpAddress("");
    	transaction.setLatitude(String.valueOf(merchant.getLocation().getLatitude()));
    	transaction.setLongitude(String.valueOf(merchant.getLocation().getLongitude()));
    	transaction.setTransactionTimeStamp(String.valueOf(currentTimeEpoch));
    	//System.out.println("To String: " + convertPOJOToJSON(transaction));
    	//transactionList.add(convertPOJOToJSON(transaction));
    	sendTransactionData(transaction);
    }
    public void generateFraudulentTransaction(Merchant merchant){
    	CreditCardTransaction transaction = new CreditCardTransaction();
    	Merchant fraudulentMerchant = new Merchant("2001","entertainment", new GeoPoint(40.750449, -73.993379)); //Madison Square Garden, NY NY)
    	Integer lowerBound = Integer.valueOf(transactionAmountByVendor.get(fraudulentMerchant.getMerchantType()).split("-")[0]) + 100;
    	Integer upperBound = Integer.valueOf(transactionAmountByVendor.get(fraudulentMerchant.getMerchantType()).split("-")[1]) + 100;
    	System.out.println("Generating Fraudulent Transaction");
    	
    	transaction.setAccountNumber(accountNumber);
    	transaction.setAccountType(accountType);
    	transaction.setMerchantId(fraudulentMerchant.getMerchantId());
    	transaction.setMerchantType(fraudulentMerchant.getMerchantType());
    	transaction.setTransactionId(fraudulentMerchant.getMerchantId()+String.valueOf(currentTimeEpoch));
    	transaction.setCurrency("USD");
    	transaction.setIsCardPresent("true");
    	transaction.setAmount(String.valueOf(random.nextInt(upperBound-lowerBound) + lowerBound));
    	transaction.setIpAddress("");
    	transaction.setLatitude(String.valueOf(fraudulentMerchant.getLocation().getLatitude()));
    	transaction.setLongitude(String.valueOf(fraudulentMerchant.getLocation().getLongitude()));
    	transaction.setTransactionTimeStamp(String.valueOf(currentTimeEpoch));
    	sendTransactionData(transaction);
    }
    
    public void sendTransactionData(CreditCardTransaction transaction){
        System.out.println("Sending Transaction Information ************************");
        
        try{
        	URL url = new URL("http://" + targetIP + ":"+targetPort+"/contentListener");
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		conn.setDoOutput(true);
    		conn.setRequestMethod("POST");
    		conn.setRequestProperty("Content-Type", "application/json");
            System.out.println("To String: " + convertPOJOToJSON(transaction));
            
            OutputStream os = conn.getOutputStream();
    		os.write(convertPOJOToJSON(transaction).getBytes());
    		os.flush();
            
            if (conn.getResponseCode() != 200)
    			throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
    		
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public long getCurrentDateEpochStart() throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date date = new Date();
		String str = dateFormat.format(date);
		Date newDate = dateFormat.parse(str);
		long eightHoursMs = 72000000;//54000000; //3PM
		long currentEpochDateEightAM = newDate.getTime() + eightHoursMs;
		
		return currentEpochDateEightAM;
		//System.out.println("Current Epoch Date 0h: " + newDate.getTime()); 
		//System.out.println("Current Epoch Date 8h: " + currentEpochDateEightAM);
		//System.out.println("Current Epoch Date +T: " + System.currentTimeMillis());
		//System.out.println("Formated Date from Epoch: " + format((newDate.getTime() + eightHoursMs)));		
	}
  
    public void simulateTransactionHistory() throws ParseException, IOException{
    	String apiKey = "";
    	String homeLat = "39.919512";
    	String homeLng = "-75.005711";
    	String googlePlacesString =null;
    	String standardizedMerchantType = null;
    	Integer merchantDiceRoll;
    	Map<String, List<Merchant>> merchantMap = new HashMap<String, List<Merchant>>();
    	List<Merchant> currentMerchantList = new ArrayList<Merchant>();
    	
    	String[] merchantArray = {"grocery_or_supermarket",
    							  "convenience_store",
    							  "clothing_store",
    							  "restaurant",
    							  "electronics_store",
    							  "bar",
    							  "gas_station",
    							  "spa|beauty_salon",
    							  "amusement_park|art_gallery|aquarium|bowling_alley|movie_theater|museum|zoo"
    							 };
    	
    	for(String currentMerchantType: merchantArray){
    		googlePlacesString = "https://maps.googleapis.com/maps/api/place/radarsearch/json?location="+homeLat+","+homeLng+"&radius=24000&type="+currentMerchantType+"&key="+apiKey;
    		
    		if(currentMerchantType.equalsIgnoreCase("spa|beauty_salon")){
        		standardizedMerchantType = "health_beauty";
        	}else if(currentMerchantType.equalsIgnoreCase("amusement_park|art_gallery|aquarium|bowling_alley|movie_theater|museum|zoo")){
        		standardizedMerchantType = "entertainment";
        	}else{
        		standardizedMerchantType = currentMerchantType;
        	}
    		merchantMap.put(standardizedMerchantType, getMerchants(googlePlacesString, currentMerchantType));
    	}
    	
    	currentTimeEpoch = simulationEpochStart + 28800000;
    	long endTime = getCurrentDateEpochStart();
    	int dailyTransactionCount = 0;
    	
    	while(currentTimeEpoch < endTime){
    		merchantDiceRoll = random.nextInt((101-1) + 1);
    		
    		if(merchantDiceRoll <= 25){
    			currentMerchantList = merchantMap.get("clothing_store");
    			generateLegitimateTransaction(currentMerchantList.get(random.nextInt((currentMerchantList.size()-1) + 1)));
    		}else if(merchantDiceRoll > 25 && merchantDiceRoll <= 45){
    			currentMerchantList = merchantMap.get("grocery_or_supermarket");
    			generateLegitimateTransaction(currentMerchantList.get(random.nextInt((currentMerchantList.size()-1) + 1)));
    		}else if(merchantDiceRoll > 45 && merchantDiceRoll <= 60){
    			currentMerchantList = merchantMap.get("restaurant");
    			generateLegitimateTransaction(currentMerchantList.get(random.nextInt((currentMerchantList.size()-1) + 1)));
    		}else if(merchantDiceRoll > 60 && merchantDiceRoll <= 75){
    			currentMerchantList = merchantMap.get("entertainment");
    			generateLegitimateTransaction(currentMerchantList.get(random.nextInt((currentMerchantList.size()-1) + 1)));
    		}else if(merchantDiceRoll > 75 && merchantDiceRoll <= 80){
    			currentMerchantList = merchantMap.get("convenience_store");
    			generateLegitimateTransaction(currentMerchantList.get(random.nextInt((currentMerchantList.size()-1) + 1)));
    		}else if(merchantDiceRoll > 80 && merchantDiceRoll <= 85){
    			currentMerchantList = merchantMap.get("electronics_store");
    			generateLegitimateTransaction(currentMerchantList.get(random.nextInt((currentMerchantList.size()-1) + 1)));
    		}else if(merchantDiceRoll > 85 && merchantDiceRoll <= 90){
    			currentMerchantList = merchantMap.get("gas_station");
    			generateLegitimateTransaction(currentMerchantList.get(random.nextInt((currentMerchantList.size()-1) + 1)));
    		}else if(merchantDiceRoll > 90 && merchantDiceRoll <= 95){
    			currentMerchantList = merchantMap.get("health_beauty");
    			generateLegitimateTransaction(currentMerchantList.get(random.nextInt((currentMerchantList.size()-1) + 1)));
    		}else if(merchantDiceRoll > 95 && merchantDiceRoll <= 100){
    			currentMerchantList = merchantMap.get("bar");
    			generateLegitimateTransaction(currentMerchantList.get(random.nextInt((currentMerchantList.size()-1) + 1)));
    		}
    		dailyTransactionCount++;
    		currentTimeEpoch += 3600000;
    		if(dailyTransactionCount == 16){
    			currentTimeEpoch += 28800000;
    			dailyTransactionCount = 0;
    		}
    	}
    	System.out.println("Total Transactions Created: " + transactionList.size());
    	Charset utf8 = StandardCharsets.UTF_8;
		Files.write(Paths.get("SimulatedTransactions.txt"), transactionList, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    
    public List<Merchant> getMerchants(String urlString, String merchantType){
    	List<Merchant> merchantList = new ArrayList<Merchant>();
    	String standardizedMerchantType;
    	
    	if(merchantType.equalsIgnoreCase("spa|beauty_salon")){
    		standardizedMerchantType = "health_beauty";
    	}else if(merchantType.equalsIgnoreCase("amusement_park|art_gallery|aquarium|bowling_alley|movie_theater|museum|zoo")){
    		standardizedMerchantType = "entertainment";
    	}else{
    		standardizedMerchantType = merchantType;
    	}
    	
    	try {
    		URL url = new URL(urlString);
            System.out.println(url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }       
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));                 
                            
            JsonFactory f = new MappingJsonFactory();
            JsonParser jp = f.createJsonParser(br);
            JsonToken current;
            current = jp.nextToken();
            if (current != JsonToken.START_OBJECT) {
                System.out.println("Error: root should be object: quiting.");
            }
            
            Merchant merchant = null;
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jp.getCurrentName();
                System.out.println(fieldName);
                merchant = new Merchant();
                // move from field name to field value
                current = jp.nextToken();
                if (current == JsonToken.START_ARRAY) {
                    // For each of the records in the array
                	while (jp.nextToken() != JsonToken.END_ARRAY) {
                		// read the record into a tree model,
                		// this moves the parsing position to the end of it
                		JsonNode node = jp.readValueAsTree();
                		System.out.println("Node: " + node);
                		// And now we have random access to everything in the object
                		printNode(node);
                		merchant.setMerchantId(node.get("place_id").getTextValue());
                		merchant.setMerchantType(standardizedMerchantType);
                		merchant.setLocation(new GeoPoint(node.get("geometry").get("location").get("lat").getDoubleValue(), 
                        					 			  node.get("geometry").get("location").get("lng").getDoubleValue()));    
                    
                        merchantList.add(merchant);
                	}
                } else {
                        System.out.println("Record should be an array: skipping.");
                        jp.skipChildren();
                }
            }
            conn.disconnect();              
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
             e.printStackTrace();
        }
        
        System.out.print(merchantList.size());
		return merchantList;
    }
            
    public static void printNode(JsonNode node) {
        Iterator<String> fieldNames = node.getFieldNames();
        while(fieldNames.hasNext()){
            String fieldName = fieldNames.next();
            JsonNode fieldValue = node.get(fieldName);
            if (fieldValue.isObject()) {
                System.out.println(fieldName + " :");
                printNode(fieldValue);
            } else {
                String value = fieldValue.asText();
                System.out.println(fieldName + " : " + value);
            }
        }
    }
    
    public String convertPOJOToJSON(Object pojo) {
    	String jsonString = "";
    	ObjectMapper mapper = new ObjectMapper();

    	try {
    		jsonString = mapper.writeValueAsString(pojo);
    	} catch (JsonGenerationException e) {
    		e.printStackTrace();
    	} catch (JsonMappingException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return jsonString;
    }
    
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getAccountNumber() {
		return accountNumber;
	}
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
	public String getAccountType() {
		return accountType;
	}
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
	public String getExpMonth() {
		return expMonth;
	}
	public void setExpMonth(String expMonth) {
		this.expMonth = expMonth;
	}
	public String getExpYear() {
		return expYear;
	}
	public void setExpYear(String expYear) {
		this.expYear = expYear;
	}
	public String getIpaddress() {
		return ipaddress;
	}
	public void setIpaddress(String ipaddress) {
		this.ipaddress = ipaddress;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
}