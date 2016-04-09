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

public class ProcessCustomerTransactionValidation extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private String customerAccountTableName = "CustomerAccount";
	private String transactionHistoryTableName = "TransactionHistory";
	private HTable customerAccountTable = null;
	private HTable transactionHistoryTable = null;
	private OutputCollector collector;
	
	public void execute(Tuple tuple) {
		CustomerResponse customerResponse = (CustomerResponse) tuple.getValueByField("CustomerResponse");
		updateCustomerAccountStatus(customerResponse.getAccountNumber(), customerResponse.getTransactionId(), Boolean.valueOf(customerResponse.getFraudulent()));
		updateTranactionStatus(customerResponse.getTransactionId(), Boolean.valueOf(customerResponse.getFraudulent()));
		System.out.println("Update Account/Transaction Status: " + customerResponse.getAccountNumber() + " Fraudulent Flag: " + Boolean.valueOf(customerResponse.getFraudulent()));
		
		collector.emit(tuple, new Values((CustomerResponse) customerResponse));
		collector.ack(tuple);
	}
	
	public void updateCustomerAccountStatus(String accountNumber, String transactionId, boolean fraudulent){
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
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", Constants.zkHost);
		config.set("hbase.zookeeper.property.clientPort", Constants.zkPort);
		config.set("zookeeper.znode.parent", "/hbase-unsecure");
		
		try {
			HBaseAdmin hbaseAdmin = new HBaseAdmin(config);
			if (hbaseAdmin.tableExists(customerAccountTableName)) {
				customerAccountTable = new HTable(config, customerAccountTableName);
			}else{
				HTableDescriptor customerAccountTableDescriptor = new HTableDescriptor(customerAccountTableName);
				HColumnDescriptor cfIIColumnFamily = new HColumnDescriptor("AccountDetails".getBytes());
				customerAccountTableDescriptor.addFamily(cfIIColumnFamily);
				customerAccountTableDescriptor.addFamily(cfIIColumnFamily);
				customerAccountTable = new HTable(config, customerAccountTableName);
				hbaseAdmin.createTable(customerAccountTableDescriptor);
			}
			
			if (hbaseAdmin.tableExists(customerAccountTableName)) {
				transactionHistoryTable = new HTable(config, transactionHistoryTableName);
			}else{
				HTableDescriptor transactionHistoryTableDescriptor = new HTableDescriptor(transactionHistoryTableName);
				HColumnDescriptor transactionsColumnFamily = new HColumnDescriptor("Transactions".getBytes());
				transactionHistoryTableDescriptor.addFamily(transactionsColumnFamily);
				transactionHistoryTable = new HTable(config, transactionHistoryTableName);
				hbaseAdmin.createTable(transactionHistoryTableDescriptor);
			}
			hbaseAdmin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("CustomerResponse"));
	}
}