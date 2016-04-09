package com.hortonworks.iot.financial;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;

@WebServlet(name = "TransactionMonitorUI", urlPatterns = { "/CustomerOverview" })
public class TransactionMonitorUI extends HttpServlet{
	private static final long serialVersionUID = 1L;
		private static final String CONTENT_TYPE = "text/html; charset=windows-1252";
	    private String requestType;
	    private HTable customerAccountTable = null;
	    private HTable transactionHistoryTable = null;
	    
	    @SuppressWarnings("deprecation")
		public void init(ServletConfig config) throws ServletException {
	    	Configuration hbaseConfig = HBaseConfiguration.create();
			
	    	super.init(config);
	        System.out.println("Calling Init method and setting request to Initial");
	        requestType = "initial";
	        //testPubSub();
	    	
	    	hbaseConfig.set("hbase.zookeeper.quorum", "sandbox.hortonworks.com");
			hbaseConfig.set("hbase.zookeeper.property.clientPort", "2181");
			hbaseConfig.set("zookeeper.znode.parent", "/hbase-unsecure");
			
			try {
				customerAccountTable = new HTable(hbaseConfig, "CustomerAccount");
				transactionHistoryTable = new HTable(hbaseConfig, "TransactionHistory");
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    public void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    	String accountNumber;
	    	AccountDetails accountDetails = null;
	    	String fraudulentTransactionId = null;
	    	Transaction fraudulentTransaction = null;
	    	List<AccountDetails> accountDetailsList = null;
	    	List<Transaction> transactionHistory = null;
	    	Map<String, Integer> merchantTypeShare = null;
	        
	    	response.setContentType(CONTENT_TYPE);
	        System.out.println("First Check of Request Type: " + requestType);
	        
	        if(request.getParameter("requestType") != null){
	            System.out.println("RequestType parameter : " + request.getParameter("requestType"));
	            requestType = request.getParameter("requestType");
	            System.out.println("RequestType set to :" + requestType);
	        }else{
	        	System.out.println("RequestType set to null, setting to initial");
	        	requestType = "initial";	            
	        	System.out.println("RequestType parameter : " + request.getParameter("requestType"));
	            System.out.println("RequestType set to :" + requestType);
	        }
	        
	        System.out.println("Checking if Initial: " + requestType);
	        if(requestType.equalsIgnoreCase("initial") || requestType.equalsIgnoreCase("customerOverview")){   
	        	//request.getRequestDispatcher("CustomerOverview.jsp").forward(request, response);
	        	request.getRequestDispatcher("CustomerOverviewInbox.jsp").forward(request, response);
	        } else if(requestType.equalsIgnoreCase("customerDetails")){   
	        	accountNumber = request.getParameter("accountNumber");
	        	accountDetails = getAccountDetails(accountNumber);
	        	transactionHistory = getTransactionHistory();
				try {
					merchantTypeShare = getMerchantTypeShare();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
	        	request.setAttribute("accountDetails", accountDetails);
	        	request.setAttribute("transactionHistory", transactionHistory);
	        	request.setAttribute("merchantTypeShare", merchantTypeShare);
	        	System.out.println("Number of transactons in history: " + transactionHistory.size());
	        	request.getRequestDispatcher("CustomerDetails.jsp").forward(request, response);
	        }else if(requestType.equalsIgnoreCase("sendFraudNotice")){
	        	accountNumber = request.getParameter("accountNumber");
	        	fraudulentTransactionId = request.getParameter("fraudulentTransactionId");
	        	accountDetails = getAccountDetails(accountNumber);
	        	accountDetails.setIsAccountActive("false");
	        	fraudulentTransaction = getTransaction(fraudulentTransactionId);
	        	fraudulentTransaction.setFraudulent("true");
	        	transactionHistory = getTransactionHistory();
	        	System.out.println("******** Sending Fraud Notification to Customer: " + fraudulentTransactionId);
	        	sendFraudNotification(fraudulentTransaction);
	        	updateAccountStatus(accountDetails);
	        	updateTransactionStatus(fraudulentTransaction);
	        	try {
					merchantTypeShare = getMerchantTypeShare();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
	        	request.setAttribute("accountDetails", accountDetails);
	        	request.setAttribute("transactionHistory", transactionHistory);
	        	request.setAttribute("merchantTypeShare", merchantTypeShare);
	        	request.getRequestDispatcher("CustomerDetails.jsp").forward(request, response);
	        }
	    }
	    private Transaction getTransaction(String transactionId) {
	    	Transaction transaction = new Transaction();

	    	Get getTransaction = new Get(Bytes.toBytes(transactionId));
		    Result result = null;
			try {
				result = transactionHistoryTable.get(getTransaction);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("accountNumber")) !=null){
				transaction.setAccountNumber(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("accountNumber"))));
				transaction.setAccountType(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("acountType"))));
				transaction.setMerchantId(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("merchantId"))));
				transaction.setMerchantType(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("merchantType"))));
				transaction.setTransactionId(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("transactionId"))));
				transaction.setAmount(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("amount"))));
				transaction.setCurrency(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("currency"))));
				transaction.setIsCardPresent(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("isCardPresent"))));
				transaction.setLatitude(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("latitude"))));
				transaction.setLongitude(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("longitude"))));
				transaction.setIpAddress(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("ipAddress"))));
				transaction.setTransactionTimeStamp(Bytes.toString(result.getValue(Bytes.toBytes("Transactions"),Bytes.toBytes("transactionTimeStampe"))));				
			}
			else{
				System.out.println("The target transaction could not be found.");
			}
	    	return transaction;
		}
	    
	    @SuppressWarnings("deprecation")
		public List<AccountDetails> getAccountDetailsList() throws IOException{
	    	AccountDetails accountDetails;
	    	List<AccountDetails> accountDetailsList = new ArrayList<AccountDetails>();
	    	System.out.println("Getting listing of customer");
	    	System.out.println("Build HBase Scanner...");
	    	Scan scan = new Scan();
	        scan.addColumn(Bytes.toBytes("AccountDetails"),Bytes.toBytes("accountNumber"));
	        scan.addColumn(Bytes.toBytes("AccountDetails"),Bytes.toBytes("accountType"));
	        scan.addColumn(Bytes.toBytes("AccountDetails"),Bytes.toBytes("isActive"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("firstName"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("lastName"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("age"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("gender"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("streetAddress"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("city"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("state"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("zipCode"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("longitude"));
		    scan.addColumn(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("latitude"));
		    
		    System.out.println("Getting Scanner...");
		    ResultScanner scanner = customerAccountTable.getScanner(scan);
		    System.out.println("Iterate through scan results");
		    for (Result result = scanner.next(); (result != null); result = scanner.next()) {
		    	accountDetails = new AccountDetails();
		    	for(KeyValue keyValue : result.list()) {
	    			System.out.println("Qualifier : " + Bytes.toString(keyValue.getQualifier()) + " : Value : " + Bytes.toString(keyValue.getValue()));
	    			if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("accountNumber")){
	    				accountDetails.setAccountNumber(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("accountType")){
	    				accountDetails.setAccountType(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("isActive")){
	    				accountDetails.setIsAccountActive(Bytes.toString(keyValue.getValue()));	
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("firstName")){
	    				accountDetails.setFirstName(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("lastName")){
	    				accountDetails.setLastName(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("age")){
	    				accountDetails.setAge(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("gender")){
	    				accountDetails.setGender(Bytes.toString(keyValue.getValue()));
	    			}else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("streetAddress")){
	    				accountDetails.setStreetAddress(Bytes.toString(keyValue.getValue()));
	    			}else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("city")){
	    				accountDetails.setCity(Bytes.toString(keyValue.getValue()));
	    			}else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("state")){
	    				accountDetails.setState(Bytes.toString(keyValue.getValue()));
	    			}else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("zipCode")){
	    				accountDetails.setZipCode(Bytes.toString(keyValue.getValue()));
	    			}else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("latitude")){
	    				accountDetails.setHomeLatitude(Bytes.toString(keyValue.getValue()));
	    			}else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("longitude")){
	    				accountDetails.setHomeLongitude(Bytes.toString(keyValue.getValue()));
	    			}
	    		}
		    	accountDetailsList.add(accountDetails);
		    }
	    	return accountDetailsList;
	    }
	    
	    public AccountDetails getAccountDetails(String accountNumber) throws IOException{
	    	AccountDetails accountDetails = new AccountDetails();

	    	Get getAccountDetails = new Get(Bytes.toBytes(accountNumber));
		    Result result = null;
			try {
				result = customerAccountTable.get(getAccountDetails);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("accountNumber")) !=null){
				accountDetails.setAccountNumber(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("accountNumber"))));
				accountDetails.setAccountType(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("accountType"))));
				accountDetails.setIsAccountActive(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("isActive"))));
				accountDetails.setFirstName(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("firstName"))));
				accountDetails.setLastName(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("lastName"))));
				accountDetails.setAge(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("age"))));
				accountDetails.setGender(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("gender"))));
				accountDetails.setStreetAddress(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("streetAddress"))));
				accountDetails.setCity(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("city"))));
				accountDetails.setState(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("state"))));
				accountDetails.setZipCode(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("zipcode"))));
				accountDetails.setHomeLatitude(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("latitude"))));
				accountDetails.setHomeLongitude(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("longitude"))));
				accountDetails.setConAmountDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("conAmtDev")))));
				accountDetails.setConAmountMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("conAmtMean")))));
				accountDetails.setDistanceDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("distanceDev")))));
				accountDetails.setDistanceMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("distanceMean")))));
				accountDetails.setElecAmountDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("elecAmtDev")))));
				accountDetails.setElecAmountMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("elecAmtMean")))));
				accountDetails.setEntAmountDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("entAmtDev")))));
				accountDetails.setEntAmountMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("entAmtMean")))));
				accountDetails.setGasAmountDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("gasAmtDev")))));
				accountDetails.setGasAmountMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("gasAmtMean")))));
				accountDetails.setGrocAmountDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("grocAmtDev")))));
				accountDetails.setGrocAmountMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("grocAmtMean")))));
				accountDetails.setHbAmountDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("hbAmtDev")))));
				accountDetails.setHbAmountMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("hbAmtMean")))));
				accountDetails.setRetAmountDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("rAmtDev")))));
				accountDetails.setRetAmountMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("rAmtMean")))));
				accountDetails.setRestAmtDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("restAmtDev")))));
				accountDetails.setRestAmtMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("restAmtMean")))));
				accountDetails.setTimeDeltaSecDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("timeDeltaSecDev")))));
				accountDetails.setTimeDetlaSecMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("timeDeltaSecMean")))));
			}else{
				System.out.println("The transaction refers to an account that is not in the data store.");
			}
	    	return accountDetails;
	    }
	    
	    @SuppressWarnings("deprecation")
		public List<Transaction> getTransactionHistory() throws IOException{
	    	Transaction transaction = null;
	    	List<Transaction> transactionList = new ArrayList<Transaction>();
	    	System.out.println("Getting Transaction History");
	    	System.out.println("Building Scanner...");
	    	Scan scan = new Scan();
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("accountNumber"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("acountType"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("frauduent"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("merchantId"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("merchantType"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("amount"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("currency"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("isCardPresent"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("latitude"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("longitude"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("ipAddress"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("transactionId"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("transactionTimeStamp"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("distanceFromHome"));
		    scan.addColumn(Bytes.toBytes("Transactions"), Bytes.toBytes("distanceFromPrev"));

		    System.out.println("Getting Scanner...");
		    ResultScanner scanner = transactionHistoryTable.getScanner(scan);
		    System.out.println("Iterate through scan results");
		    for (Result result = scanner.next(); (result != null); result = scanner.next()) {
		    	transaction = new Transaction();
		    	for(KeyValue keyValue : result.list()) {
	    			System.out.println("Qualifier : " + Bytes.toString(keyValue.getQualifier()) + " : Value : " + Bytes.toString(keyValue.getValue()));
	    			if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("accountNumber")){
	    				transaction.setAccountNumber(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("accountType")){
	    				transaction.setAccountType(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("merchantId")){
	    				transaction.setMerchantId(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("merchantType")){
	    				transaction.setMerchantType(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("frauduent")){
	    				transaction.setFraudulent(Bytes.toString(keyValue.getValue()));
		    		} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("transactionId")){
		    			transaction.setTransactionId(Bytes.toString(keyValue.getValue()));
		    		} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("amount")){
	    				transaction.setAmount(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("currency")){
	    				transaction.setCurrency(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("isCardPresent")){
	    				transaction.setIsCardPresent(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("latitude")){
	    				transaction.setLatitude(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("longitude")){
	    				transaction.setLongitude(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("ipAddress")){
	    				transaction.setIpAddress(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("transactionId")){
	    				transaction.setTransactionId(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("transactionTimeStamp")){
	    				transaction.setTransactionTimeStamp(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("distanceFromHome")){
	    				transaction.setDistanceFromHome(Bytes.toString(keyValue.getValue()));
	    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("distanceFromPrev")){
	    				transaction.setDistanceFromPrev(Bytes.toString(keyValue.getValue()));
	    			}	
		    	}
		    	transactionList.add(transaction);
		    }	
		    return transactionList;
	    }
	    public Map<String, Integer> getMerchantTypeShare() throws ClassNotFoundException, SQLException{
	    	Map<String, Integer> merchantTypeShare = new HashMap<String, Integer>();
	    	Connection conn;
	        Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
	        conn =  DriverManager.getConnection("jdbc:phoenix:sandbox.hortonworks.com:2181:/hbase-unsecure");
	        System.out.println("got connection");
	        ResultSet rst = conn.createStatement().executeQuery("SELECT \"merchantType\", COUNT(\"merchantType\") as \"Count\" FROM \"TransactionHistory\" WHERE \"frauduent\" = 'false' GROUP BY \"merchantType\"");
	        while (rst.next()) {
	        	merchantTypeShare.put(rst.getString(1), rst.getInt(2));
	        	System.out.println(rst.getString(1) + " " + rst.getString(2));
	        }
	        
	        return merchantTypeShare;
	    }
	    
	    public void updateTransactionStatus(Transaction transaction){
			Put transactionToUpdate = new Put(Bytes.toBytes(transaction.getTransactionId()));
			transactionToUpdate.add(Bytes.toBytes("Transactions"), Bytes.toBytes("frauduent"), Bytes.toBytes(transaction.getFraudulent()));
			
			try {
				transactionHistoryTable.put(transactionToUpdate);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    
	    public void updateAccountStatus(AccountDetails account){
			Put accountToUpdate = new Put(Bytes.toBytes(account.getAccountNumber()));
			accountToUpdate.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("isActive"), Bytes.toBytes(account.getIsAccountActive()));
			
			try {
				customerAccountTable.put(accountToUpdate);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    
	    public void sendFraudNotification(Transaction transaction){
	        System.out.println("Sending Customer Notification Information ************************");
	        transaction.setSource("analyst_action");
	        transaction.setFraudulent("true");
	        try{
	        	URL url = new URL("http://sandbox.hortonworks.com:8082/contentListener");
	    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    		conn.setDoOutput(true);
	    		conn.setRequestMethod("POST");
	    		conn.setRequestProperty("Content-Type", "application/json");
	            String payload = "{\"to\": \"/topics/fraudAlert\",\"data\": " + convertPOJOToJSON(transaction) + "}"; 
	    		System.out.println("To String: " + payload);
	            
	            OutputStream os = conn.getOutputStream();
	    		os.write(payload.getBytes());
	    		os.flush();
	            
	            if (conn.getResponseCode() != 200)
	    			throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
	    		
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	    
	    public void testPubSub() {
	    	String pubSubUrl = "http://sandbox.hortonworks.com:8091/cometd";
	    	String fraudAlertChannel = "/fraudAlert";
	    	HttpClient httpClient = new HttpClient();
			try {
				httpClient.start();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Prepare the transport
			Map<String, Object> options = new HashMap<String, Object>();
			ClientTransport transport = new LongPollingTransport(options, httpClient);

			// Create the BayeuxClient
			BayeuxClient bayuexClient = new BayeuxClient(pubSubUrl, transport);
			
			bayuexClient.handshake();
			boolean handshaken = bayuexClient.waitFor(5000, BayeuxClient.State.CONNECTED);
			if (handshaken)
			{
				System.out.println("Connected to Cometd Http PubSub Platform");
			}
			else{
				System.out.println("Could not connect to Cometd Http PubSub Platform");
			}
			
			bayuexClient.getChannel(fraudAlertChannel).publish("TEST");
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
	    
		public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	        this.doTask(request, response);
	    }
	    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	       this.doTask(request, response);
	    }
}