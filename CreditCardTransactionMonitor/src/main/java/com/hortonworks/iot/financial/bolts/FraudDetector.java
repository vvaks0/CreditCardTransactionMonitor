package com.hortonworks.iot.financial.bolts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
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

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class FraudDetector extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	private HTable transactionHistoryTable = null;
	private HTable customerAccountTable = null;
	private SVMModel svm;
	private String pathToWeights = "/demo/models/modelWeights"; 
	
	//private SparkContext sc;
	
	public void execute(Tuple tuple)  {
		EnrichedTransaction transaction = (EnrichedTransaction) tuple.getValueByField("EnrichedTransaction");
		EnrichedTransaction previousTransaction = null;
		
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
		collector.ack(tuple);
	}
	
	@SuppressWarnings("deprecation")
	public void persistTransactionToHbase(EnrichedTransaction transaction){
		Put transactionToPersist = new Put(Bytes.toBytes(transaction.getTransactionId()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("accountNumber"), Bytes.toBytes(transaction.getAccountNumber()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("acountType"), Bytes.toBytes(transaction.getAccountType()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("frauduent"), Bytes.toBytes(transaction.getFraudulent()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("merchantId"), Bytes.toBytes(transaction.getMerchantId()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("merchantType"), Bytes.toBytes(transaction.getMerchantType()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("amount"), Bytes.toBytes(transaction.getAmount()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("currency"), Bytes.toBytes(transaction.getCurrency()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("isCardPresent"), Bytes.toBytes(transaction.getIsCardPresent()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("latitude"), Bytes.toBytes(transaction.getLatitude()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("longitude"), Bytes.toBytes(transaction.getLongitude()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("ipAddress"), Bytes.toBytes(transaction.getIpAddress()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("transactionId"), Bytes.toBytes(transaction.getTransactionId()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("transactionTimeStamp"), Bytes.toBytes(transaction.getTransactionTimeStamp()));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("distanceFromHome"), Bytes.toBytes(String.valueOf(transaction.getDistanceFromHome())));
		transactionToPersist.add(Bytes.toBytes("Transactions"), Bytes.toBytes("distanceFromPrev"), Bytes.toBytes(String.valueOf(transaction.getDistanceFromPrev())));
		try {
			transactionHistoryTable.put(transactionToPersist);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
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
		config.set("hbase.zookeeper.quorum", Constants.zkHost);
		config.set("hbase.zookeeper.property.clientPort", Constants.zkPort);
		config.set("zookeeper.znode.parent", "/hbase-unsecure");
		
	    // Instantiating HTable
		try {
			HBaseAdmin hbaseAdmin = new HBaseAdmin(config);
			if (hbaseAdmin.tableExists("TransactionHistory")) {
				transactionHistoryTable = new HTable(config, "TransactionHistory");
			}else{
				HTableDescriptor tableDescriptor = new HTableDescriptor("TransactionHistory");
				HColumnDescriptor cfColumnFamily = new HColumnDescriptor("Transactions".getBytes());
		        tableDescriptor.addFamily(cfColumnFamily);
		        hbaseAdmin.createTable(tableDescriptor);
		        transactionHistoryTable = new HTable(config, "TransactionHistory");
			}
			
			if (hbaseAdmin.tableExists("CustomerAccount")) {
				customerAccountTable = new HTable(config, "CustomerAccount");
			}else{
				HTableDescriptor tableDescriptor = new HTableDescriptor("CustomerAccount");
				HColumnDescriptor cfColumnFamily = new HColumnDescriptor("CustomerDetails".getBytes());
				HColumnDescriptor cfIIColumnFamily = new HColumnDescriptor("AccountDetails".getBytes());
				tableDescriptor.addFamily(cfColumnFamily);
				tableDescriptor.addFamily(cfIIColumnFamily);
				customerAccountTable = new HTable(config, "CustomerAccount");
				hbaseAdmin.createTable(tableDescriptor);
			}
			hbaseAdmin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Connection conn;
        try {
			Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
			conn = DriverManager.getConnection("jdbc:phoenix:sandbox.hortonworks.com:2181:/hbase-unsecure");
			conn.createStatement().executeUpdate("CREATE VIEW \"TransactionHistory\" (pk VARCHAR PRIMARY KEY, \"Transactions\".\"merchantType\" VARCHAR, \"Transactions\".\"frauduent\" VARCHAR");
			conn.commit();
        } catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}        
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream("LegitimateTransactionStream", new Fields("EnrichedTransaction"));
		declarer.declareStream("FraudulentTransactionStream", new Fields("EnrichedTransaction"));
	}
}