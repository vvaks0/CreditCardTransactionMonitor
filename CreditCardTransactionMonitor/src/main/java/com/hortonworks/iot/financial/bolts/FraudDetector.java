package com.hortonworks.iot.financial.bolts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.mllib.classification.SVMModel;
import org.apache.spark.mllib.classification.SVMWithSGD;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;

import com.hortonworks.iot.financial.events.EnrichedTransaction;
import com.hortonworks.iot.financial.util.Constants;
import com.hortonworks.iot.financial.util.Model;
import com.hortonworks.iot.financial.util.Profile;
import com.hortonworks.iot.financial.util.StormProvenanceEvent;

import clojure.lang.BigInt;

/*
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
*/

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;


public class FraudDetector extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	private HTable transactionHistoryTable = null;
	private HTable customerAccountTable = null;
	private SVMModel svm;
	private String pathToWeights = "/demo/models/modelWeights";
	private String componentId;
	private String componentType;
	private Constants constants;
	private Connection conn = null;
	
	//private SparkContext sc;
	
	public void execute(Tuple tuple)  {
		EnrichedTransaction transaction = (EnrichedTransaction) tuple.getValueByField("EnrichedTransaction");
		EnrichedTransaction previousTransaction = null;
		
		String actionType = "SEND";
		List<StormProvenanceEvent> stormProvenance = (List<StormProvenanceEvent>)tuple.getValueByField("ProvenanceEvent");
		String transactionKey = stormProvenance.get(0).getEventKey();
	    StormProvenanceEvent provenanceEvent = new StormProvenanceEvent(transactionKey, actionType, componentId, componentType);
	    provenanceEvent.setTargetDataRepositoryType("HBASE");
	    provenanceEvent.setTargetDataRepositoryLocation(constants.getZkConnString() + ":" + constants.getZkHBasePath() + ":" + transactionHistoryTable.getName().getNameAsString());
	    stormProvenance.add(provenanceEvent);
		
		try {
			previousTransaction = getLastTransaction(transaction.getAccountNumber()); 
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Model model = new Model(svm);
		transaction = model.calculateFraudScore(transaction, previousTransaction);
		System.out.println("**********************Probability Transaction is Fraudulent: " + transaction.getScore());
		
		if(transaction.getScore() < 50){
			transaction.setFraudulent("false");
			persistTransactionToHbase(transaction);
			collector.emit("LegitimateTransactionStream", new Values(transaction));
		}else{
			transaction.setFraudulent("true");
			persistTransactionToHbase(transaction);
			collector.emit("FraudulentTransactionStream", new Values(transaction));
		}
		/*collector.emit("HiveTransactionStream", new Values(transaction.getAccountNumber(),
																 transaction.getAccountType(),
																 transaction.getFraudulent(),
																 transaction.getMerchantId(),
																 transaction.getMerchantType(),
																 transaction.getAmount(),
																 transaction.getCurrency(),
																 transaction.getIsCardPresent(),
																 transaction.getLatitude(),
																 transaction.getLongitude(),
																 transaction.getTransactionId(),
																 transaction.getTransactionTimeStamp(),
																 transaction.getDistanceFromHome(),
																 transaction.getDistanceFromPrev())); */
		collector.emit("ProvenanceRegistrationStream", new Values(stormProvenance));
		collector.ack(tuple);
	}
	
	@SuppressWarnings("deprecation")
	public void persistTransactionToHbase(EnrichedTransaction transaction){
		
		String currentTransaction = "UPSERT INTO \"TransactionHistory\" VALUES('"+transaction.getAccountNumber()+"','"+
																				   transaction.getAccountType()+"','"+
																				   transaction.getMerchantId()+"','"+
																				   transaction.getMerchantType()+"','"+
																				   transaction.getFraudulent()+"',"+
																				   transaction.getAmount()+",'"+
																				   transaction.getCurrency()+"','"+
																				   transaction.getIpAddress()+"','" +
																				   transaction.getIsCardPresent()+"'," +
																				   transaction.getLatitude()+"," +
																				   transaction.getLongitude()+",'"+
																				   transaction.getTransactionId()+"',"+
																				   transaction.getTransactionTimeStamp()+","+
																				   transaction.getDistanceFromHome()+","+
																				   transaction.getDistanceFromPrev()+")";
		System.out.println("*************** UPSERT STATEMENT: " + currentTransaction);
		try {
			conn.createStatement().executeUpdate(currentTransaction);
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		/*
		Put transactionToPersist = new Put(Bytes.toBytes(transaction.getTransactionId()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("accountNumber"), Bytes.toBytes(String.valueOf(transaction.getAccountNumber())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("acountType"), Bytes.toBytes(String.valueOf(transaction.getAccountType())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("frauduent"), Bytes.toBytes(String.valueOf(transaction.getFraudulent())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("merchantId"), Bytes.toBytes(String.valueOf(transaction.getMerchantId())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("merchantType"), Bytes.toBytes(String.valueOf(transaction.getMerchantType())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("amount"), Bytes.toBytes(Double.valueOf(transaction.getAmount())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("currency"), Bytes.toBytes(String.valueOf(transaction.getCurrency())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("isCardPresent"), Bytes.toBytes(String.valueOf(transaction.getIsCardPresent())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("latitude"), Bytes.toBytes(Double.valueOf(transaction.getLatitude())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("longitude"), Bytes.toBytes(Double.valueOf(transaction.getLongitude())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("ipAddress"), Bytes.toBytes(String.valueOf(transaction.getIpAddress())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("transactionId"), Bytes.toBytes(String.valueOf(transaction.getTransactionId())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("transactionTimeStamp"), Bytes.toBytes(Long.valueOf(transaction.getTransactionTimeStamp())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("distanceFromHome"), Bytes.toBytes(Double.valueOf(transaction.getDistanceFromHome())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("distanceFromPrev"), Bytes.toBytes(Double.valueOf(transaction.getDistanceFromPrev())));
		try {
			transactionHistoryTable.put(transactionToPersist);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}
	
	@SuppressWarnings("deprecation")
	public EnrichedTransaction getLastTransaction(String accountNumber) throws IOException{
		EnrichedTransaction currentTransaction = new EnrichedTransaction();
		EnrichedTransaction lastTransaction = new EnrichedTransaction();
		long currentTimeStamp = 0;
		
		lastTransaction.setAccountNumber(accountNumber);
		lastTransaction.setLatitude("0.0");
		lastTransaction.setLongitude("0.0");
		
		Scan scan = new Scan();
		scan.addColumn(Bytes.toBytes("Transactions"),Bytes.toBytes("transactionId"));
	    scan.addColumn(Bytes.toBytes("Transactions"),Bytes.toBytes("latitude"));
	    scan.addColumn(Bytes.toBytes("Transactions"),Bytes.toBytes("longitude"));
	    scan.addColumn(Bytes.toBytes("Transactions"),Bytes.toBytes("transactionTimeStamp"));
	    scan.addColumn(Bytes.toBytes("Transactions"),Bytes.toBytes("frauduent"));
	    
	    ResultScanner scanner = transactionHistoryTable.getScanner(scan);
	    // Scanning the required columns
	    
	    for (Result result = scanner.next(); (result != null); result = scanner.next()) {
            for(KeyValue keyValue : result.list()) {
    			//System.out.println("Qualifier : " + Bytes.toString(keyValue.getQualifier()) + " : Value : " + Bytes.toString(keyValue.getValue()));
    			if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("latitude")){
    				currentTransaction.setLatitude(Bytes.toString(keyValue.getValue()));
    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("longitude")){
    				currentTransaction.setLongitude(Bytes.toString(keyValue.getValue()));
    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("transactionTimeStamp")){
    				currentTransaction.setTransactionTimeStamp(Bytes.toString(keyValue.getValue()));
    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("transactionId")){
    				currentTransaction.setTransactionId(Bytes.toString(keyValue.getValue()));
    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("frauduent")){
    				currentTransaction.setFraudulent(Bytes.toString(keyValue.getValue()));
    			}
    		}
            System.out.println("*************** Transaction : Current: " + currentTransaction.getTransactionId() + " : Existing: " + lastTransaction.getTransactionId());
            System.out.println("*************** Time Stamp Compare : Current: " + result.raw()[0].getTimestamp() + " : Existing: " + currentTimeStamp);
            if(currentTimeStamp <= Long.valueOf(result.raw()[0].getTimestamp()) && !Boolean.valueOf(currentTransaction.getFraudulent())){
    			currentTimeStamp = Long.valueOf(result.raw()[0].getTimestamp());
    			lastTransaction.setLatitude(currentTransaction.getLatitude());
    			lastTransaction.setLongitude(currentTransaction.getLongitude());
    			lastTransaction.setTransactionId(currentTransaction.getTransactionId());
    			lastTransaction.setTransactionTimeStamp(currentTransaction.getTransactionTimeStamp());
    			System.out.println("*************** Wrote Current to Existing : Current: " + currentTransaction.getTransactionId() + " : Existing: " + lastTransaction.getTransactionId());
    		}
	    }
	    System.out.println("*************** Last Transaction: " + lastTransaction.getTransactionId());
		return lastTransaction;
	}
	
	public Double distanceFrom(Double lat1, Double lng1, Double lat2, Double lng2) {
		double earthRadius = 6371000; //meters
		double dLat = Math.toRadians(lat2-lat1);
		double dLng = Math.toRadians(lng2-lng1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
				Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
				Math.sin(dLng/2) * Math.sin(dLng/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double dist = (Double)(earthRadius * c);

		return dist;
	}
	
	@SuppressWarnings("deprecation")
	public void prepare(Map arg0, TopologyContext context, OutputCollector collector) {
		this.constants = new Constants();
		this.componentId = context.getThisComponentId();
		this.componentType = "BOLT";
		this.collector = collector;
		double intercept = -1.7535004133947043;
		double [] weights = {0.17751022275925205,0.060917670306210064,0.10924794398547727};
		svm = new SVMWithSGD().createModel(Vectors.dense(weights), intercept);
		svm.clearThreshold();
		//SparkConf sparkConf = new SparkConf().setMaster("local[4]").setAppName("Nostradamus");
		//sc = new SparkContext(sparkConf);
		//System.out.print("********************* SparkContext Home:" + sc.getSparkHome());
		//final SVMModel nostradamus = SVMModel.load(sc, "nostradamusSVMModel");
		
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", constants.getZkHost());
		config.set("hbase.zookeeper.property.clientPort", constants.getZkPort());
		config.set("zookeeper.znode.parent", constants.getZkHBasePath());
		
	    // Instantiating HTable
		try {
			HBaseAdmin hbaseAdmin = new HBaseAdmin(config);
			if (hbaseAdmin.tableExists("TransactionHistory")) {
				transactionHistoryTable = new HTable(config, "TransactionHistory");
				Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
				conn = DriverManager.getConnection("jdbc:phoenix:"+ constants.getZkHost() + ":" + constants.getZkPort() + ":" + constants.getZkHBasePath());
			}else{
				Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
				conn = DriverManager.getConnection("jdbc:phoenix:"+ constants.getZkHost() + ":" + constants.getZkPort() + ":" + constants.getZkHBasePath());
				conn.createStatement().executeUpdate("create table if not exists \"TransactionHistory\" "
						+ "(pk VARCHAR PRIMARY KEY, "
						+ "\"Transactions\".\"accountNumber\" VARCHAR, "
						+ "\"Transactions\".\"accountType\" VARCHAR, "
						+ "\"Transactions\".\"merchantId\" VARCHAR, "
						+ "\"Transactions\".\"merchantType\" VARCHAR, "
						+ "\"Transactions\".\"frauduent\" VARCHAR, "
						+ "\"Transactions\".\"amount\" DOUBLE, "
						+ "\"Transactions\".\"currency\" VARCHAR, "
						+ "\"Transactions\".\"ipAddress\" VARCHAR, "
						+ "\"Transactions\".\"isCardPresent\" VARCHAR, "
						+ "\"Transactions\".\"latitude\" DOUBLE, "
						+ "\"Transactions\".\"longitude\" DOUBLE, "
						+ "\"Transactions\".\"transactionId\" VARCHAR, "
						+ "\"Transactions\".\"transactionTimeStamp\" BIGINT, "
						+ "\"Transactions\".\"distanceFromHome\" DOUBlE, "
						+ "\"Transactions\".\"distanceFromPrev\" DOUBLE)");
				conn.commit();
				
				while(!hbaseAdmin.tableExists("TransactionHistory")){ 
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("******************** EnrichTransaction prepare() Waiting for Phoenix Tables to be created..."); 
				}
				System.out.println("******************** EnrichTransaction prepare() Phoenix Tables created...");
				
				transactionHistoryTable = new HTable(config, "TransactionHistory");
				/* transactionHistoryTable = new HTable(config, "TransactionHistory");
				HTableDescriptor tableDescriptor = new HTableDescriptor("TransactionHistory");
				HColumnDescriptor cfColumnFamily = new HColumnDescriptor("Transactions".getBytes());
		        tableDescriptor.addFamily(cfColumnFamily);
		        hbaseAdmin.createTable(tableDescriptor);
		        transactionHistoryTable = new HTable(config, "TransactionHistory"); */
			}
			
			if (hbaseAdmin.tableExists("CustomerAccount")) {
				customerAccountTable = new HTable(config, "CustomerAccount");
			}else{
				HTableDescriptor tableDescriptor = new HTableDescriptor("CustomerAccount");
				HColumnDescriptor cfColumnFamily = new HColumnDescriptor("CustomerDetails".getBytes());
				HColumnDescriptor cfIIColumnFamily = new HColumnDescriptor("AccountDetails".getBytes());
				tableDescriptor.addFamily(cfColumnFamily);
				tableDescriptor.addFamily(cfIIColumnFamily);
				hbaseAdmin.createTable(tableDescriptor);
				customerAccountTable = new HTable(config, "CustomerAccount");
			}
			hbaseAdmin.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream("LegitimateTransactionStream", new Fields("EnrichedTransaction"));
		declarer.declareStream("FraudulentTransactionStream", new Fields("EnrichedTransaction"));
		declarer.declareStream("ProvenanceRegistrationStream", new Fields("ProvenanceEvent"));
		declarer.declareStream("NormalizedTransactionStream", new Fields("accountNumber",
																 "accountType",
																 "fraudulent",
																 "merchantId",
																 "merchantType",
																 "amount",
																 "currency",
																 "isCardPresent",
																 "latitude",
																 "longitude",
																 "transactionId",
																 "transactionTimeStamp",
																 "distanceFromHome",
																 "distanceFromPrev"));
		
		declarer.declareStream("HiveTransactionStream", new Fields("accountnumber",
					"accounttype",
					"fraudulent",
					"merchantid",
					"merchanttype",
					"amount",
					"currency",
					"iscardpresent",
					"latitude",
					"longitude",
					"transactionid",
					"transactiontimestamp",
					"distancefromhome",
					"distancefromprev"));
	}
}