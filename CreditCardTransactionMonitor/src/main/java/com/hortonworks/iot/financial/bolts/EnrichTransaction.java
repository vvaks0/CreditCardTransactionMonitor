package com.hortonworks.iot.financial.bolts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import com.hortonworks.iot.financial.events.EnrichedTransaction;
import com.hortonworks.iot.financial.events.IncomingTransaction;
import com.hortonworks.iot.financial.util.Constants;
import com.hortonworks.iot.financial.util.StormProvenanceEvent;

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


public class EnrichTransaction extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private String componentId;
	private String componentType;
	//private String tableName = "CustomerAccount";
   // private HTable table = null;
	private OutputCollector collector;
	private Constants constants;
	private HTable customerAccountTable = null;
	
	public void execute(Tuple tuple) {
		IncomingTransaction incomingTransaction = (IncomingTransaction) tuple.getValueByField("IncomingTransaction");
		EnrichedTransaction enrichedTransaction = new EnrichedTransaction();
		
		String actionType = "MODIFY";
		List<StormProvenanceEvent> stormProvenance = (List<StormProvenanceEvent>)tuple.getValueByField("ProvenanceEvent");
		String transactionKey = stormProvenance.get(0).getEventKey();
		
		StormProvenanceEvent provenanceEvent = new StormProvenanceEvent(transactionKey, actionType, componentId, componentType);
	    stormProvenance.add(provenanceEvent);
		
		enrichedTransaction.setAccountNumber(incomingTransaction.getAccountNumber());
		enrichedTransaction.setAccountType(incomingTransaction.getAccountType());
		enrichedTransaction.setAmount(incomingTransaction.getAmount());
		enrichedTransaction.setCurrency(incomingTransaction.getCurrency());
		enrichedTransaction.setIsCardPresent(incomingTransaction.getIsCardPresent());
		enrichedTransaction.setLatitude(incomingTransaction.getLatitude());
		enrichedTransaction.setLongitude(incomingTransaction.getLongitude());
		enrichedTransaction.setMerchantId(incomingTransaction.getMerchantId());
		enrichedTransaction.setMerchantType(incomingTransaction.getMerchantType());
		enrichedTransaction.setTransactionId(incomingTransaction.getTransactionId());
		enrichedTransaction.setTransactionTimeStamp(incomingTransaction.getTransactionTimeStamp());
		enrichedTransaction.setIpAddress(incomingTransaction.getIpAddress());
		
		System.out.println("********************** Enriching event: " + transactionKey);
	    Get get = new Get(Bytes.toBytes(incomingTransaction.getAccountNumber()));
	    Result result = null;
		try {
			result = customerAccountTable.get(get);
		} catch (IOException e) {
			System.out.println("********************** Event: " + transactionKey + ": could not access HBase");
			e.printStackTrace();
		}
		
		if(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("accountNumber")) !=null){
			enrichedTransaction.setIsAccountActive(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("isActive"))));
			enrichedTransaction.setFirstName(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("firstName"))));
			enrichedTransaction.setLastName(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("lastName"))));
			enrichedTransaction.setAge(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("age"))));
			enrichedTransaction.setGender(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("gender"))));
			enrichedTransaction.setStreetAddress(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("streetAddress"))));
			enrichedTransaction.setCity(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("city"))));
			enrichedTransaction.setState(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("state"))));
			enrichedTransaction.setZipCode(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("zipcode"))));
			enrichedTransaction.setHomeLatitude(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("latitude"))));
			enrichedTransaction.setHomeLongitude(Bytes.toString(result.getValue(Bytes.toBytes("CustomerDetails"),Bytes.toBytes("longitude"))));
			enrichedTransaction.setConAmtDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("conAmtDev")))));
			enrichedTransaction.setConAmtMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("conAmtMean")))));
			enrichedTransaction.setDistanceDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("distanceDev")))));
			enrichedTransaction.setDistanceMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("distanceMean")))));
			enrichedTransaction.setElecAmtDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("elecAmtDev")))));
			enrichedTransaction.setElecAmtMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("elecAmtMean")))));
			enrichedTransaction.setEntAmtDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("entAmtDev")))));
			enrichedTransaction.setEntAmtMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("entAmtMean")))));
			enrichedTransaction.setGasAmtDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("gasAmtDev")))));
			enrichedTransaction.setGasAmtMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("gasAmtMean")))));
			enrichedTransaction.setGrocAmtDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("grocAmtDev")))));
			enrichedTransaction.setGrocAmtMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("grocAmtMean")))));
			enrichedTransaction.setHbAmtDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("hbAmtDev")))));
			enrichedTransaction.setHbAmtMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("hbAmtMean")))));
			enrichedTransaction.setrAmtDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("rAmtDev")))));
			enrichedTransaction.setrAmtMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("rAmtMean")))));
			enrichedTransaction.setRestAmtDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("restAmtDev")))));
			enrichedTransaction.setRestAmtMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("restAmtMean")))));
			enrichedTransaction.setTimeDeltaSecDev(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("timeDeltaSecDev")))));
			enrichedTransaction.setTimeDetlaSecMean(Double.valueOf(Bytes.toString(result.getValue(Bytes.toBytes("AccountDetails"),Bytes.toBytes("timeDeltaSecMean")))));
			
			System.out.println("*************** " + enrichedTransaction.getConAmtDev());
			System.out.println("*************** " + enrichedTransaction.getConAmtMean());
			System.out.println("*************** " + enrichedTransaction.getDistanceDev());
			System.out.println("*************** " + enrichedTransaction.getDistanceMean());
			System.out.println("*************** " + enrichedTransaction.getElecAmtDev());
			System.out.println("*************** " + enrichedTransaction.getElecAmtMean());
			System.out.println("*************** " + enrichedTransaction.getEntAmtDev());
			System.out.println("*************** " + enrichedTransaction.getEntAmtMean());
			System.out.println("*************** " + enrichedTransaction.getGasAmtDev());
			System.out.println("*************** " + enrichedTransaction.getGasAmtMean());
			System.out.println("*************** " + enrichedTransaction.getGrocAmtDev());
			System.out.println("*************** " + enrichedTransaction.getGrocAmtMean());
			System.out.println("*************** " + enrichedTransaction.getHbAmtDev());
			System.out.println("*************** " + enrichedTransaction.getHbAmtMean());
			System.out.println("*************** " + enrichedTransaction.getrAmtDev());
			System.out.println("*************** " + enrichedTransaction.getrAmtMean());
			System.out.println("*************** " + enrichedTransaction.getRestAmtDev());
			System.out.println("*************** " + enrichedTransaction.getRestAmtMean());
			System.out.println("*************** " + enrichedTransaction.getTimeDeltaSecDev());
			System.out.println("*************** " + enrichedTransaction.getTimeDetlaSecMean());
			
			collector.emit(tuple, new Values((EnrichedTransaction)enrichedTransaction, stormProvenance));
			collector.ack(tuple);
		}
		else{
			System.out.println("The transaction refers to an account that is not in the data store.");
			System.out.println("Account: " + incomingTransaction.getAccountNumber());
			collector.ack(tuple);
		}
	}

	@SuppressWarnings("deprecation")
	public void prepare(Map arg0, TopologyContext context, OutputCollector collector) {
		this.constants = new Constants();
		this.componentId = context.getThisComponentId();
		this.componentType = "BOLT";
		
		System.out.println("********************** Zookeeper Host: " + constants.getZkHost());
        System.out.println("********************** Zookeeper Port: " + constants.getZkPort());
        System.out.println("********************** Zookeeper ConnString: " + constants.getZkConnString());
        System.out.println("********************** Zookeeper Kafka Path: " + constants.getZkKafkaPath());
        System.out.println("********************** Zookeeper HBase Path: " + constants.getZkHBasePath());
        System.out.println("********************** Cometd URI: " + constants.getPubSubUrl());
		
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", constants.getZkHost());
		config.set("hbase.zookeeper.property.clientPort", constants.getZkPort());
		config.set("zookeeper.znode.parent", constants.getZkHBasePath());
		
		try {
			HBaseAdmin hbaseAdmin = new HBaseAdmin(config);
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
		}

		Put customer1000 = new Put(Bytes.toBytes("19123"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("firstName"), Bytes.toBytes("Regina"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("lastName"), Bytes.toBytes("Smith"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("age"), Bytes.toBytes("32"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("gender"), Bytes.toBytes("Female"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("streetAddress"), Bytes.toBytes("1234 Tampa Ave."));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("city"), Bytes.toBytes("Cherry Hill"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("state"), Bytes.toBytes("NJ"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("zipcode"), Bytes.toBytes("08003"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("latitude"), Bytes.toBytes("39.919512"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("longitude"), Bytes.toBytes("-75.005711"));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("ipAddress"), Bytes.toBytes(""));
		customer1000.add(Bytes.toBytes("CustomerDetails"), Bytes.toBytes("port"), Bytes.toBytes(""));
		
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("accountNumber"), Bytes.toBytes("19123"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("accountType"), Bytes.toBytes("VISA"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("expMonth"), Bytes.toBytes("09"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("expYear"), Bytes.toBytes("19"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("accountLimit"), Bytes.toBytes("20000"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("isActive"), Bytes.toBytes("true"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("distanceMean"), Bytes.toBytes("9.173712305"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("distanceDev"), Bytes.toBytes("5.968364997"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("timeDeltaSecMean"), Bytes.toBytes("5398.577075"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("timeDeltaSecDev"), Bytes.toBytes("6968.79762"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("conAmtMean"), Bytes.toBytes("16.87915743"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("conAmtDev"), Bytes.toBytes("4.272822919"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("gasAmtMean"), Bytes.toBytes("36.9679558"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("gasAmtDev"), Bytes.toBytes("7.226414921"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("rAmtMean"), Bytes.toBytes("174.1947298"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("rAmtDev"), Bytes.toBytes("72.17713403"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("elecAmtMean"), Bytes.toBytes("98.97291196"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("elecAmtDev"), Bytes.toBytes("29.0160567"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("entAmtMean"), Bytes.toBytes("39.4743295"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("entAmtDev"), Bytes.toBytes("5.728492345"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("hbAmtMean"), Bytes.toBytes("84.07411631"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("hbAmtDev"), Bytes.toBytes("35.58637624"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("restAmtMean"), Bytes.toBytes("73.73396065"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("restAmtDev"), Bytes.toBytes("38.0403594"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("grocAmtMean"), Bytes.toBytes("83.73396065"));
		customer1000.add(Bytes.toBytes("AccountDetails"), Bytes.toBytes("grocAmtDev"), Bytes.toBytes("32.0403594"));
											
		try {
			customerAccountTable.put(customer1000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("EnrichedTransaction","ProvenanceEvent"));
	}
}