package com.hortonworks.iot.financial.topology;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.storm.hbase.bolt.HBaseBolt;
import org.apache.storm.hbase.bolt.mapper.SimpleHBaseMapper;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy.Units;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;

/*
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kafka.BrokerHosts;
import org.apache.storm.kafka.KafkaSpout;
import org.apache.storm.kafka.KeyValueSchemeAsMultiScheme;
import org.apache.storm.kafka.SpoutConfig;
import org.apache.storm.kafka.ZkHosts;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
*/

import com.hortonworks.iot.financial.bolts.AtlasLineageReporter;
import com.hortonworks.iot.financial.bolts.EnrichTransaction;
import com.hortonworks.iot.financial.bolts.FraudDetector;
import com.hortonworks.iot.financial.bolts.InstantiateProvenance;
import com.hortonworks.iot.financial.bolts.PrintTransaction;
import com.hortonworks.iot.financial.bolts.ProcessCustomerTransactionValidation;
import com.hortonworks.iot.financial.bolts.PublishAccountStatusUpdate;
import com.hortonworks.iot.financial.bolts.PublishFraudAlert;
import com.hortonworks.iot.financial.bolts.PublishTransaction;
import com.hortonworks.iot.financial.util.Constants;
import com.hortonworks.iot.financial.util.CustomerResponseEventJSONScheme;
import com.hortonworks.iot.financial.util.TransactionEventJSONScheme;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.AuthorizationException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.KeyValueSchemeAsMultiScheme;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;


public class CreditCardTransactionMonitorTopology {
	 public static void main(String[] args) {
	     TopologyBuilder builder = new TopologyBuilder();
	     Constants constants = new Constants();   
	     // Use pipe as record boundary
	  	  RecordFormat format = new DelimitedRecordFormat().withFieldDelimiter(",");

	  	  //Synchronize data buffer with the filesystem every 1000 tuples
	  	  SyncPolicy syncPolicy = new CountSyncPolicy(1000);

	  	  // Rotate data files when they reach five MB
	  	  FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(5.0f, Units.MB);

	  	  // Use default, Storm-generated file names
	  	  FileNameFormat transactionLogFileNameFormat = new DefaultFileNameFormat().withPath(constants.getHivePath());
	  	  HdfsBolt LogTransactionHdfsBolt = new HdfsBolt()
	  		     .withFsUrl(constants.getNameNode())
	  		     .withFileNameFormat(transactionLogFileNameFormat)
	  		     .withRecordFormat(format)
	  		     .withRotationPolicy(rotationPolicy)
	  		     .withSyncPolicy(syncPolicy);
	  	System.out.println("********************** Starting Topology.......");
	  	System.out.println("********************** Zookeeper Host: " + constants.getZkHost());
        System.out.println("********************** Zookeeper Port: " + constants.getZkPort());
        System.out.println("********************** Zookeeper ConnString: " + constants.getZkConnString());
        System.out.println("********************** Zookeeper Kafka Path: " + constants.getZkKafkaPath());
        System.out.println("********************** Zookeeper HBase Path: " + constants.getZkHBasePath());
        System.out.println("********************** Cometd URI: " + constants.getPubSubUrl());
	  	  
	      Config conf = new Config(); 
	      //BrokerHosts hosts = new ZkHosts(Constants.zkConnString);
	      BrokerHosts hosts = new ZkHosts(constants.getZkConnString(), constants.getZkKafkaPath());
	      
	      //SpoutConfig incomingTransactionsKafkaSpoutConfig = new SpoutConfig(hosts, Constants.incomingTransactionsTopicName, "/" + Constants.incomingTransactionsTopicName, UUID.randomUUID().toString());
	      SpoutConfig incomingTransactionsKafkaSpoutConfig = new SpoutConfig(hosts, constants.getIncomingTransactionsTopicName(), constants.getZkKafkaPath(), UUID.randomUUID().toString());
	      incomingTransactionsKafkaSpoutConfig.scheme = new KeyValueSchemeAsMultiScheme(new TransactionEventJSONScheme());
	      incomingTransactionsKafkaSpoutConfig.ignoreZkOffsets = true;
	      incomingTransactionsKafkaSpoutConfig.useStartOffsetTimeIfOffsetOutOfRange = true;
	      incomingTransactionsKafkaSpoutConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
	      KafkaSpout incomingTransactionsKafkaSpout = new KafkaSpout(incomingTransactionsKafkaSpoutConfig); 
	      
	      //SpoutConfig customerTransactionValidationKafkaSpoutConfig = new SpoutConfig(hosts, Constants.customerTransactionValidationTopicName, "/" + Constants.customerTransactionValidationTopicName, UUID.randomUUID().toString());
	      SpoutConfig customerTransactionValidationKafkaSpoutConfig = new SpoutConfig(hosts, constants.getCustomerTransactionValidationTopicName(), constants.getZkKafkaPath(), UUID.randomUUID().toString());
	      customerTransactionValidationKafkaSpoutConfig.scheme = new SchemeAsMultiScheme(new CustomerResponseEventJSONScheme());
	      customerTransactionValidationKafkaSpoutConfig.ignoreZkOffsets = true;
	      customerTransactionValidationKafkaSpoutConfig.useStartOffsetTimeIfOffsetOutOfRange = true;
	      customerTransactionValidationKafkaSpoutConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
	      KafkaSpout customerTransactionValidationKafkaSpout = new KafkaSpout(customerTransactionValidationKafkaSpoutConfig);
	      
	      Map<String, Object> hbConf = new HashMap<String, Object>();
	      hbConf.put("hbase.rootdir", constants.getNameNode() + "/apps/hbase/data/");
	      hbConf.put("hbase.zookeeper.quorum", constants.getZkHost());
		  hbConf.put("hbase.zookeeper.property.clientPort", constants.getZkPort());
	      hbConf.put("zookeeper.znode.parent", constants.getZkHBasePath());
	      conf.put("hbase.conf", hbConf);
	      
	      SimpleHBaseMapper transactionMapper = new SimpleHBaseMapper()
	              .withRowKeyField("transactionId")
	              .withColumnFields(new Fields("accountNumber",
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
	            		  					   "distanceFromPrev"))
	              .withColumnFamily("Transactions");	    
	      
	      builder.setSpout("IncomingTransactionsKafkaSpout", incomingTransactionsKafkaSpout);
	      builder.setBolt("InstantiateProvenance", new InstantiateProvenance(), 1).shuffleGrouping("IncomingTransactionsKafkaSpout");
	      builder.setBolt("EnrichTransaction", new EnrichTransaction(), 1).shuffleGrouping("InstantiateProvenance");      
	      //builder.setBolt("PublishTransaction", new PublishTransaction(), 1).shuffleGrouping("EnrichTransaction");
	      builder.setBolt("DetectFraud", new FraudDetector(), 1).shuffleGrouping("EnrichTransaction");
	      builder.setBolt("PersistTransactionToHBase", new HBaseBolt("TransactionHistory", transactionMapper).withConfigKey("hbase.conf"), 1).shuffleGrouping("DetectFraud", "NormalizedTransactionStream");
	      builder.setBolt("PublishFraudAlert", new PublishFraudAlert(), 1).shuffleGrouping("DetectFraud", "FraudulentTransactionStream");
	      builder.setBolt("PublishTransaction", new PublishTransaction(), 1).shuffleGrouping("DetectFraud", "LegitimateTransactionStream");
	      builder.setBolt("AtlasLineageReporter", new AtlasLineageReporter(), 1).shuffleGrouping("DetectFraud", "ProvenanceRegistrationStream");
	      
	      builder.setSpout("CustomerTransactionValidationKafkaSpout", customerTransactionValidationKafkaSpout);
	      //builder.setSpout("CustomerTransactionValidationKafkaSpout", new KafkaSpout(), 1);
	      builder.setBolt("ProcessCustomerTransactionValidation", new ProcessCustomerTransactionValidation(), 1).shuffleGrouping("CustomerTransactionValidationKafkaSpout");
	      builder.setBolt("PublishAccountStatusUpdate", new PublishAccountStatusUpdate(), 1).shuffleGrouping("CustomerTransactionValidationKafkaSpout");
	      
	      conf.setNumWorkers(1);
	      conf.setMaxSpoutPending(5000);
	      conf.setMaxTaskParallelism(1);
	      
	      //submitToLocal(builder, conf);
	      submitToCluster(builder, conf);
	 }
	 
	 public static void submitToLocal(TopologyBuilder builder, Config conf){
		 LocalCluster cluster = new LocalCluster();
		 cluster.submitTopology("CreditCardTransactionMonitor", conf, builder.createTopology()); 
	 }
	 
	 public static void submitToCluster(TopologyBuilder builder, Config conf){
		 try {
				StormSubmitter.submitTopology("CreditCardTransactionMonitor", conf, builder.createTopology());
		      } catch (AlreadyAliveException e) {
				e.printStackTrace();
		      } catch (InvalidTopologyException e) {
				e.printStackTrace();
		      } catch (AuthorizationException e) {
				e.printStackTrace();
		      }
	 }
}
