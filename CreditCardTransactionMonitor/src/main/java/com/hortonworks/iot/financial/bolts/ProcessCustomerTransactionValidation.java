package com.hortonworks.iot.financial.bolts;

import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.hortonworks.iot.financial.events.CustomerResponse;
import com.hortonworks.iot.financial.events.EnrichedTransaction;
import com.hortonworks.iot.financial.events.IncomingTransaction;
import com.hortonworks.iot.financial.util.Constants;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/*
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
*/

public class ProcessCustomerTransactionValidation extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private String customerAccountTableName = "CustomerAccount";
	private String transactionHistoryTableName = "TransactionHistory";
	private HTable customerAccountTable = null;
	private HTable transactionHistoryTable = null;
	private OutputCollector collector;
	private Constants constants;
	
	public void execute(Tuple tuple) {
		CustomerResponse customerResponse = (CustomerResponse) tuple.getValueByField("CustomerResponse");
		updateCustomerAccountStatus(customerResponse.getAccountNumber(), customerResponse.getTransactionId(), Boolean.valueOf(customerResponse.getFraudulent()));
		updateTranactionStatus(customerResponse.getTransactionId(), Boolean.valueOf(customerResponse.getFraudulent()));
		System.out.println("********************* Update Account/Transaction Status: " + customerResponse.getAccountNumber() + " Fraudulent Flag: " + Boolean.valueOf(customerResponse.getFraudulent()));
		
		collector.emit(tuple, new Values((CustomerResponse) customerResponse));
		collector.ack(tuple);
	}
	
	public void updateCustomerAccountStatus(String accountNumber, String transactionId, boolean fraudulent){
		System.out.println("********************* Updating Accout Status based on Customer feedback: " + accountNumber + " : " + transactionId + " : " + fraudulent);
		Put customerAccount = new Put(Bytes.toBytes(accountNumber));
		
		if(fraudulent){
			customerAccount.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("isActive"), Bytes.toBytes("false"));
		}else{
			customerAccount.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("isActive"), Bytes.toBytes("true"));
		}
		
		try {
			customerAccountTable.put(customerAccount);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void updateTranactionStatus(String transactionId, boolean fraudulent){
		Put transaction = new Put(Bytes.toBytes(transactionId));
		
		if(fraudulent){
			transaction.add(Bytes.toBytes("Transactions"), Bytes.toBytes("frauduent"), Bytes.toBytes("true"));
		}else{
			transaction.add(Bytes.toBytes("Transactions"), Bytes.toBytes("frauduent"), Bytes.toBytes("false"));
		}
		
		try {
			transactionHistoryTable.put(transaction);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("deprecation")
	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.constants = new Constants();
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", constants.getZkHost());
		config.set("hbase.zookeeper.property.clientPort", constants.getZkPort());
		config.set("zookeeper.znode.parent", constants.getZkHBasePath());
		
		System.out.println("********************* Preparing " + customerAccountTableName + " Table ....");
		try {
			HBaseAdmin hbaseAdmin = new HBaseAdmin(config);
			
			System.out.println("********************* CustomerAccount Table Exists? : " + hbaseAdmin.tableExists(customerAccountTableName));
			if(hbaseAdmin.tableExists(customerAccountTableName)) {	
				customerAccountTable = new HTable(config, customerAccountTableName);
				System.out.println("********************* ProcessCustomerValidationBolt Prepare() has aquired " + customerAccountTableName + " table.");
			}else{
				while(!hbaseAdmin.tableExists(customerAccountTableName)){
					System.out.println("********************* ProcessCustomerValidationBolt Prepare() is waiting for " + customerAccountTableName + " table to be created....");
					Thread.sleep(5000);
				}
				customerAccountTable = new HTable(config, customerAccountTableName);
				System.out.println("********************* ProcessCustomerValidationBolt Prepare() has aquired " + customerAccountTableName + " table.");
			}
			
			System.out.println("********************* Preparing " + transactionHistoryTableName + "Table ....");
			System.out.println("********************* TransactionHistory Table Exists? : " + hbaseAdmin.tableExists(transactionHistoryTableName));
			if(hbaseAdmin.tableExists(transactionHistoryTableName)) {
				transactionHistoryTable = new HTable(config, transactionHistoryTableName);
				System.out.println("********************* ProcessCustomerValidationBolt Prepare() has aquired " + transactionHistoryTableName + " table.");
			}else{
				while(!hbaseAdmin.tableExists(transactionHistoryTableName)) {
					System.out.println("********************* ProcessCustomerValidationBolt Prepare() is waiting for " + transactionHistoryTableName + " table to be created....");
					Thread.sleep(5000);
				}
				transactionHistoryTable = new HTable(config, transactionHistoryTableName);
				System.out.println("********************* ProcessCustomerValidationBolt Prepare() has aquired " + transactionHistoryTableName + " table.");
			}
			hbaseAdmin.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 

		this.collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("CustomerResponse"));
	}
}