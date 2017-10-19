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
	    private String zkHost = "sandbox.hortonworks.com";
	    private String zkPort = "2181";
	    private String zkHBasePath = "/hbase-unsecure";
	    private String httpHost = "sandbox.hortonworks.com";
	    private String httpListenPort = "8082";
	    private String httpListenUri = "/contentListener";
	    private String cometdHost = "sandbox.hortonworks.com";
	    private String cometdListenPort = "8091";
		private String defaultAccountNumber = "19123";
		private String mapAPIKey = "NO_API_KEY_FOUND";
		private boolean SAMDemo = false;
		private Connection conn;
	    
	    @SuppressWarnings("deprecation")
		public void init(ServletConfig config) throws ServletException {
	    	Configuration hbaseConfig = HBaseConfiguration.create();
			
	    	super.init(config);
	        System.out.println("Calling Init method and setting request to Initial");
	        requestType = "initial";
	        //testPubSub();
	        
	        Map<String, String> env = System.getenv();
	        System.out.println("********************** ENV: " + env);
	        if(env.get("ZK_HOST") != null){
	        	this.zkHost = (String)env.get("ZK_HOST");
	        }
	        if(env.get("ZK_PORT") != null){
	        	this.zkPort = (String)env.get("ZK_PORT");
	        }
	        if(env.get("ZK_HBASE_PATH") != null){
	        	this.zkHBasePath = (String)env.get("ZK_HBASE_PATH");
	        }
	        if(env.get("COMETD_HOST") != null){
	        	this.cometdHost = (String)env.get("COMETD_HOST");
	        }
	        if(env.get("COMETD_PORT") != null){
	        	this.cometdListenPort = (String)env.get("COMETD_PORT");
	        }
	        if(env.get("HTTP_HOST") != null){
	        	this.httpHost = (String)env.get("HTTP_HOST");
	        }
	        if(env.get("HTTP_PORT") != null){
	        	this.httpListenPort = (String)env.get("HTTP_PORT");
	        }
	        if(env.get("HTTP_URI") != null){
	        	this.httpListenUri = (String)env.get("HTTP_URI");
	        }
	        if(env.get("MAP_API_KEY") != null){
	        	this.mapAPIKey  = (String)env.get("MAP_API_KEY");
	        }
	        if(env.get("SAM_DEMO") != null){
	        	this.SAMDemo  = Boolean.valueOf((String)env.get("SAM_DEMO"));
	        }
	        System.out.println("********************** Zookeeper Host: " + zkHost);
	        System.out.println("********************** Zookeeper: " + zkPort);
	        System.out.println("********************** Zookeeper Path: " + zkHBasePath);
	        System.out.println("********************** Cometd Host: " + cometdHost);
	        System.out.println("********************** Cometd Port: " + cometdListenPort);
	        System.out.println("********************** Http Host: " + httpHost);
	        System.out.println("********************** Http Port: " + httpListenPort);
	        System.out.println("********************** Http Uri: " + httpListenUri);
	        System.out.println("********************** Map Api Key: " + mapAPIKey);
	        System.out.println("********************** SAM Demo: " + mapAPIKey);
	        
	    	hbaseConfig.set("hbase.zookeeper.quorum", zkHost);
			hbaseConfig.set("hbase.zookeeper.property.clientPort", zkPort);
			hbaseConfig.set("zookeeper.znode.parent", zkHBasePath);
			
			try {
				Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
				conn =  DriverManager.getConnection("jdbc:phoenix:" + zkHost + ":" + zkPort + ":" + zkHBasePath);
		        System.out.println("got connection");
		        customerAccountTable = new HTable(hbaseConfig, "CustomerAccount");
				transactionHistoryTable = new HTable(hbaseConfig, "TransactionHistory");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
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
	        	accountDetails = getAccountDetailsSQL(defaultAccountNumber );
	        	transactionHistory = getTransactionHistorySQL();
	        	request.setAttribute("cometdHost", cometdHost);
	        	request.setAttribute("cometdPort", cometdListenPort);
	        	request.setAttribute("mapAPIKey", mapAPIKey);
	        	request.setAttribute("accountDetails", accountDetails);
	        	request.setAttribute("transactionHistory", transactionHistory);
	        	request.getRequestDispatcher("CustomerOverviewInbox.jsp").forward(request, response);
	        } else if(requestType.equalsIgnoreCase("customerDetails")){   
	        	accountNumber = request.getParameter("accountNumber");
	        	accountDetails = getAccountDetailsSQL(accountNumber);
	        	transactionHistory = getTransactionHistorySQL();
				try {
					merchantTypeShare = getMerchantTypeShare();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				request.setAttribute("cometdHost", cometdHost);
	        	request.setAttribute("cometdPort", cometdListenPort);
	        	request.setAttribute("mapAPIKey", mapAPIKey);
	        	request.setAttribute("accountDetails", accountDetails);
	        	request.setAttribute("transactionHistory", transactionHistory);
	        	request.setAttribute("merchantTypeShare", merchantTypeShare);
	        	System.out.println("Number of transactons in history: " + transactionHistory.size());
	        	request.getRequestDispatcher("CustomerDetails.jsp").forward(request, response);
	        }else if(requestType.equalsIgnoreCase("sendFraudNotice")){
	        	accountNumber = request.getParameter("accountNumber");
	        	fraudulentTransactionId = request.getParameter("fraudulentTransactionId");
	        	accountDetails = getAccountDetailsSQL(accountNumber);
	        	accountDetails.setIsAccountActive("false");
	        	fraudulentTransaction = getTransactionSQL(fraudulentTransactionId);
	        	fraudulentTransaction.setFraudulent("true");
	        	transactionHistory = getTransactionHistorySQL();
	        	System.out.println("******** Sending Fraud Notification to Customer: " + fraudulentTransactionId);
	        	sendFraudNotification(fraudulentTransaction);
	        	updateAccountStatusSQL(accountDetails);
	        	updateTransactionStatusSQL(fraudulentTransaction);
	        	try {
					merchantTypeShare = getMerchantTypeShare();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
	        	request.setAttribute("cometdHost", cometdHost);
	        	request.setAttribute("cometdPort", cometdListenPort);
	        	request.setAttribute("mapAPIKey", mapAPIKey);
	        	request.setAttribute("accountDetails", accountDetails);
	        	request.setAttribute("transactionHistory", transactionHistory);
	        	request.setAttribute("merchantTypeShare", merchantTypeShare);
	        	request.getRequestDispatcher("CustomerDetails.jsp").forward(request, response);
	        }
	    }
	    private Transaction getTransactionSQL(String transactionId) {
	    	Transaction transaction = new Transaction();
	    	List<Transaction> transactionList = new ArrayList<Transaction>();
	    	String transactionQueryString = null;
	    	transactionQueryString = "SELECT TRANSACTIONID, ACCOUNTNUMBER, ACCOUNTTYPE, FRAUDULENT, "
	    										+ "MERCHANTID, MERCHANTTYPE, AMOUNT, CURRENCY, LATITUDE, LONGITUDE, "
	    										+ "TRANSACTIONTIMESTAMP, DISTANCEFROMHOME, DISTANCEFROMPREV"
	    										+ "FROM TRANSACTIONHISTORY"
	    										+ "WHERE TRANSACTIONID = '"+transactionId+"'";
	    		transactionQueryString = "SELECT PK, "
					+ "\"accountNumber\","
					+ "\"accountType\","
					+ "\"frauduent\","
					+ "\"merchantId\","
					+ "\"merchantType\","
					+ "\"amount\","
					+ "\"currency\","
					+ "\"isCardPresent\","
					+ "\"latitude\","
					+ "\"longitude\","
					+ "\"transactionId\","
					+ "\"transactionTimeStamp\","
					+ "\"distanceFromHome\","
					+ "\"distanceFromPrev\" "
					+ " FROM \"TransactionHistory\" "
					+ " WHERE \"transactionId\" = '" + transactionId + "'";
	    	ResultSet rst;
			try {
				rst = conn.createStatement().executeQuery(transactionQueryString);
				while (rst.next()) {
					transaction = new Transaction();
					transaction.setAccountNumber(rst.getString("accountnumber"));
					transaction.setAccountType(rst.getString("accounttype"));
					transaction.setMerchantId(rst.getString("merchantid"));
					transaction.setMerchantType(rst.getString("merchanttype"));
					transaction.setFraudulent(rst.getString("frauduent"));
					transaction.setTransactionId(rst.getString("transactionid"));
					transaction.setAmount(String.valueOf(rst.getDouble("amount")));
					transaction.setCurrency(rst.getString("currency"));
					transaction.setLatitude(String.valueOf(rst.getDouble("latitude")));
					transaction.setLongitude(String.valueOf(rst.getDouble("longitude")));
					transaction.setTransactionId(rst.getString("transactionid"));
					transaction.setTransactionTimeStamp(String.valueOf(rst.getLong("transactiontimestamp")));
					transaction.setDistanceFromHome(String.valueOf(rst.getDouble("distancefromhome")));
					transaction.setDistanceFromPrev(String.valueOf(rst.getDouble("distancefromprev")));
							
					transactionList.add(transaction);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
	    	return transactionList.get(0);
		}
	    
	    public List<Transaction> getTransactionHistorySQL() throws IOException{
	    	Transaction transaction = null;
	    	List<Transaction> transactionList = new ArrayList<Transaction>();
	    	String transactionQueryString = null;
	    	transactionQueryString = "SELECT TRANSACTIONID, ACCOUNTNUMBER, ACCOUNTTYPE, FRAUDULENT, "
	    										+ "MERCHANTID, MERCHANTTYPE, AMOUNT, CURRENCY, LATITUDE, LONGITUDE, "
	    										+ "TRANSACTIONTIMESTAMP, DISTANCEFROMHOME, DISTANCEFROMPREV "
	    										+ "FROM TRANSACTIONHISTORY";
	    	
	    	/*String transactionQueryString = "SELECT "
					+ "\"accountNumber\","
					+ "\"accountType\","
					+ "\"frauduent\","
					+ "\"merchantId\","
					+ "\"merchantType\","
					+ "\"amount\","
					+ "\"currency\","
					+ "\"isCardPresent\","
					+ "\"latitude\","
					+ "\"longitude\","
					+ "\"transactionId\","
					+ "\"transactionTimeStamp\","
					+ "\"distanceFromHome\","
					+ "\"distanceFromPrev\" "
					+ " FROM \"TransactionHistory\" ";*/
	    	
		    ResultSet rst;
			try {
				rst = conn.createStatement().executeQuery(transactionQueryString);
				while (rst.next()) {
					transaction = new Transaction();
					transaction.setAccountNumber(rst.getString("accountType"));
					transaction.setAccountType(rst.getString("accountType"));
					transaction.setMerchantId(rst.getString("merchantId"));
					transaction.setMerchantType(rst.getString("merchantType"));
					transaction.setFraudulent(rst.getString("fraudulent"));
					transaction.setTransactionId(rst.getString("transactionId"));
					transaction.setAmount(String.valueOf(rst.getDouble("amount")));
					transaction.setCurrency(rst.getString("currency"));
					transaction.setLatitude(String.valueOf(rst.getDouble("latitude")));
					transaction.setLongitude(String.valueOf(rst.getDouble("longitude")));
					transaction.setTransactionId(rst.getString("transactionId"));
					transaction.setTransactionTimeStamp(rst.getString("transactionTimeStamp"));
					transaction.setDistanceFromHome(String.valueOf(rst.getDouble("distanceFromHome")));
					transaction.setDistanceFromPrev(String.valueOf(rst.getDouble("distanceFromPrev")));
						
					transactionList.add(transaction);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		    	
		    return transactionList;
	    }
	    
	    public List<AccountDetails> getAccountDetailsListSQL() throws IOException{
	    	AccountDetails accountDetails = null;
	    	List<AccountDetails> accountDetailsList = new ArrayList<AccountDetails>();
	    	String queryString = null;
	    	queryString = "SELECT ACCOUNTNUMBER, ACCOUNTTYPE, ISACTIVE, FIRSTNAME, LASTNAME, AGE, GENDER, "
	    			+ "STREETADDRESS, CITY, STATE, ZIPCODE, LONGITUDE, LATITUDE FROM CUSTOMERACCOUNT";
	    	ResultSet rst;
			try {
				rst = conn.createStatement().executeQuery(queryString);
				while (rst.next()) {
					accountDetails = new AccountDetails();
					accountDetails.setAccountNumber(rst.getString("accountnumber"));
					accountDetails.setAccountType(rst.getString("accounttype"));
					accountDetails.setIsAccountActive(rst.getString("isactive"));	
					accountDetails.setFirstName(rst.getString("firstname"));
					accountDetails.setLastName(rst.getString("lastname"));
					accountDetails.setAge(rst.getString("age"));
					accountDetails.setGender(rst.getString("gender"));
					accountDetails.setStreetAddress(rst.getString("streetaddress"));
					accountDetails.setCity(rst.getString("city"));
					accountDetails.setState(rst.getString("state"));
					accountDetails.setZipCode(rst.getString("zipcode"));
					accountDetails.setHomeLatitude(rst.getString("latitide"));
					accountDetails.setHomeLongitude(rst.getString("longitude"));
		    	}
			    accountDetailsList.add(accountDetails);
			} catch (SQLException e) {
				e.printStackTrace();
			}
	    	
	    	return accountDetailsList;
	    }
	    
	    public AccountDetails getAccountDetailsSQL(String accountNumber) throws IOException{
	    	AccountDetails accountDetails = null;
	    	String queryString = null;
	    	queryString = "SELECT ACCOUNTNUMBER, ACCOUNTTYPE, ISACTIVE, FIRSTNAME, LASTNAME, AGE, GENDER, "
	    			+ "STREETADDRESS, CITY, STATE, ZIPCODE, LONGITUDE, LATITUDE FROM CUSTOMERACCOUNT WHERE ACCOUNTNUMBER = '"+accountNumber+"'";
	    	ResultSet rst;
			try {
				rst = conn.createStatement().executeQuery(queryString);
				while (rst.next()) {
					accountDetails = new AccountDetails();
					accountDetails.setAccountNumber(rst.getString("accountnumber"));
					accountDetails.setAccountType(rst.getString("accounttype"));
					accountDetails.setIsAccountActive(rst.getString("isactive"));	
					accountDetails.setFirstName(rst.getString("firstname"));
					accountDetails.setLastName(rst.getString("lastname"));
					accountDetails.setAge(rst.getString("age"));
					accountDetails.setGender(rst.getString("gender"));
					accountDetails.setStreetAddress(rst.getString("streetaddress"));
					accountDetails.setCity(rst.getString("city"));
					accountDetails.setState(rst.getString("state"));
					accountDetails.setZipCode(rst.getString("zipcode"));
					accountDetails.setHomeLatitude(rst.getString("latitude"));
					accountDetails.setHomeLongitude(rst.getString("longitude"));
		    	}
			} catch (SQLException e) {
				e.printStackTrace();
			}
	    	
	    	return accountDetails;
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
	    	/*
	    	ResultSet rst;
			try {
				rst = conn.createStatement().executeQuery("SELECT \"merchantType\", COUNT(\"merchantType\") as \"Count\" FROM \"TransactionHistory\" WHERE \"frauduent\" = 'false' GROUP BY \"merchantType\"");
				while (rst.next()) {
					accountDetails.setAccountNumber(rst.getString("accountNumber"));
					accountDetails.setAccountType(rst.getString("accountType"));
					accountDetails.setIsAccountActive(rst.getString("isActive"));
					accountDetails.setFirstName(rst.getString("firstName"));
					accountDetails.setLastName(rst.getString("lastName"));
					accountDetails.setAge(rst.getString("age"));
					accountDetails.setGender(rst.getString("gender"));
					accountDetails.setStreetAddress(rst.getString("streetAddress"));
					accountDetails.setCity(rst.getString("city"));
					accountDetails.setState(rst.getString("state"));
					accountDetails.setZipCode(rst.getString("zipcode"));
					accountDetails.setHomeLatitude(rst.getString("latitude"));
					accountDetails.setHomeLongitude(rst.getString("longitude"));
					accountDetails.setConAmountDev(rst.getDouble("conAmtDev"));
					accountDetails.setConAmountMean(rst.getDouble("conAmtMean"));
					accountDetails.setDistanceDev(rst.getDouble("distanceDev"));
					accountDetails.setDistanceMean(rst.getDouble("distanceMean"));
					accountDetails.setElecAmountDev(rst.getDouble("elecAmtDev"));
					accountDetails.setElecAmountMean(rst.getDouble("elecAmtMean"));
					accountDetails.setEntAmountDev(rst.getDouble("entAmtDev"));
					accountDetails.setEntAmountMean(rst.getDouble("entAmtMean"));
					accountDetails.setGasAmountDev(rst.getDouble("gasAmtDev"));
					accountDetails.setGasAmountMean(rst.getDouble("gasAmtMean"));
					accountDetails.setGrocAmountDev(rst.getDouble("grocAmtDev"));
					accountDetails.setGrocAmountMean(rst.getDouble("grocAmtMean"));
					accountDetails.setHbAmountDev(rst.getDouble("hbAmtDev"));
					accountDetails.setHbAmountMean(rst.getDouble("hbAmtMean"));
					accountDetails.setRetAmountDev(rst.getDouble("rAmtDev"));
					accountDetails.setRetAmountMean(rst.getDouble("rAmtMean"));
					accountDetails.setRestAmtDev(rst.getDouble("restAmtDev"));
					accountDetails.setRestAmtMean(rst.getDouble("restAmtMean"));
					accountDetails.setTimeDeltaSecDev(rst.getDouble("timeDeltaSecDev"));
					accountDetails.setTimeDetlaSecMean(rst.getDouble("timeDeltaSecMean"));
				}
			}catch(Exception e){
				e.printStackTrace();
			}*/
	    	
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
		
	    public Map<String, Integer> getMerchantTypeShare() throws ClassNotFoundException, SQLException{
	    	Map<String, Integer> merchantTypeShare = new HashMap<String, Integer>();
	        ResultSet rst = conn.createStatement().executeQuery("SELECT MERCHANTTYPE, COUNT(MERCHANTTYPE) AS MERCHANTCOUNT "
	        		+ "FROM TRANSACTIONHISTORY WHERE FRAUDULENT = 'false' GROUP BY MERCHANTTYPE");
	        while (rst.next()) {
	        	merchantTypeShare.put(rst.getString(1), rst.getInt(2));
	        	System.out.println(rst.getString(1) + " " + rst.getString(2));
	        }
	        
	        return merchantTypeShare;
	    }
	    
	    public void updateTransactionStatusSQL(Transaction transaction){
	    	String queryString = null;
	    	queryString = "UPSERT INTO TRANSACTIONHISTORY (TRANSACTIONID, FRAUDULENT) "
	    			+ "VALUES ('"+transaction.getTransactionId()+"','"+transaction.getFraudulent()+"')";
			try {
				conn.createStatement().executeUpdate(queryString);
			} catch (SQLException e) {
				e.printStackTrace();
			}
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
	    
	    public void updateAccountStatusSQL(AccountDetails account){
	    	String queryString = null;
	    	queryString = "UPSERT INTO CUSTOMERACCOUNT (ACCOUNTNUMBER, ISACTIVE) "
	    			+ "VALUES ('"+account.getAccountNumber()+"','"+account.getIsAccountActive()+"')";
			try {
				conn.createStatement().executeUpdate(queryString);
			} catch (SQLException e) {
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
	        	URL url = new URL("http://" + httpHost + ":" + httpListenPort + httpListenUri);
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
	    	String pubSubUrl = "http://" + cometdHost + ":" + cometdListenPort + "/cometd";
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