package com.hortonworks.iot.financial.util;

import java.util.Map;

public class Constants {
	private String zkHost = "sandbox.hortonworks.com";
	private String zkPort = "2181";
	private String zkKafkaPath = "/brokers";
	private String zkHBasePath = "/hbase-unsecure";
	private String zkConnString;
	private String incomingTransactionsTopicName = "IncomingTransactions";
	private String customerTransactionValidationTopicName = "CustomerTransactionValidation";
	
	private String cometdHost = "sandbox.hortonworks.com";
	private String cometdPort = "8091";
	private String pubSubUrl;
	private String incomingTransactionsChannel = "/incomingTransactions";
	private String fraudAlertChannel = "/fraudAlert";
	private String accountStatusUpdateChannel = "/accountStatusUpdate";
	
	private String nameNodeHost = "sandbox.hortonworks.com";
	private String nameNodePort = "8020";
	private String nameNode;
	private String hivePath = "/demo/data/transaction_logs";
	
	private String atlasHost = "localhost";
	private String atlasPort = "21000";
	
	private String hiveMetaStoreURI = "jdbc:mysql://sandbox.hortonworks.com/hive";
	private String hiveDbName = "default";
	
	public Constants(){
		Map<String, String> env = System.getenv();
        //System.out.println("********************** ENV: " + env);
        if(env.get("ZK_HOST") != null){
        	this.zkHost = (String)env.get("ZK_HOST");
        }
        if(env.get("ZK_PORT") != null){
        	this.zkPort = (String)env.get("ZK_PORT");
        }
        if(env.get("ZK_KAFKA_PATH") != null){
        	this.zkKafkaPath = (String)env.get("ZK_KAFKA_PATH");
        }
        if(env.get("ZK_HBASE_PATH") != null){
        	this.zkHBasePath = (String)env.get("ZK_HBASE_PATH");
        }
        if(env.get("NAME_NODE_HOST") != null){
        	this.nameNodeHost = (String)env.get("NAME_NODE_HOST");
        }
        if(env.get("NAME_NODE_PORT") != null){
        	this.nameNodePort = (String)env.get("NAME_NODE_PORT");
        }
        if(env.get("HIVE_PATH") != null){
        	this.hivePath = (String)env.get("HIVE_PATH");
        }
        if(env.get("HIVE_METASTORE_URI") != null){
        	this.hiveMetaStoreURI = (String)env.get("HIVE_METASTORE_URI");
        }
        if(env.get("ATLAS_HOST") != null){
        	this.setAtlasHost((String)env.get("ATLAS_HOST"));
        }
        if(env.get("ATLAS_PORT") != null){
        	this.setAtlasPort((String)env.get("ATLAS_PORT"));
        }
        if(env.get("COMETD_HOST") != null){
        	this.cometdHost = (String)env.get("COMETD_HOST");
        }
        if(env.get("COMETD_PORT") != null){
        	this.cometdPort = (String)env.get("COMETD_PORT");
        }
        
        this.zkConnString = zkHost+":"+zkPort;
        this.pubSubUrl = "http://" + cometdHost + ":" + cometdPort + "/cometd";
        this.nameNode = "hdfs://" + nameNodeHost + ":" + nameNodePort;
	}

	public String getZkHost() {
		return zkHost;
	}

	public void setZkHost(String zkHost) {
		this.zkHost = zkHost;
	}
	
	public String getZkPort() {
		return zkPort;
	}

	public void setZkPort(String zkPort) {
		this.zkPort = zkPort;
	}

	public String getZkKafkaPath() {
		return zkKafkaPath;
	}

	public void setZkKafkaPath(String zkKafkaPath) {
		this.zkKafkaPath = zkKafkaPath;
	}
	
	public String getZkHBasePath() {
		return zkHBasePath;
	}

	public void setZkHBasePath(String zkHBasePath) {
		this.zkHBasePath = zkHBasePath;
	}

	public String getZkConnString() {
		return zkConnString;
	}

	public void setZkConnString(String zkConnString) {
		this.zkConnString = zkConnString;
	}

	public String getPubSubUrl() {
		return pubSubUrl;
	}

	public void setPubSubUrl(String pubSubUrl) {
		this.pubSubUrl = pubSubUrl;
	}

	public String getIncomingTransactionsChannel() {
		return incomingTransactionsChannel;
	}

	public void setIncomingTransactionsChannel(String incomingTransactionsChannel) {
		this.incomingTransactionsChannel = incomingTransactionsChannel;
	}

	public String getFraudAlertChannel() {
		return fraudAlertChannel;
	}

	public void setFraudAlertChannel(String fraudAlertChannel) {
		this.fraudAlertChannel = fraudAlertChannel;
	}

	public String getAccountStatusUpdateChannel() {
		return accountStatusUpdateChannel;
	}

	public void setAccountStatusUpdateChannel(String accountStatusUpdateChannel) {
		this.accountStatusUpdateChannel = accountStatusUpdateChannel;
	}

	public String getIncomingTransactionsTopicName() {
		return incomingTransactionsTopicName;
	}

	public void setIncomingTransactionsTopicName(String incomingTransactionsTopicName) {
		this.incomingTransactionsTopicName = incomingTransactionsTopicName;
	}

	public String getCustomerTransactionValidationTopicName() {
		return customerTransactionValidationTopicName;
	}

	public void setCustomerTransactionValidationTopicName(String customerTransactionValidationTopicName) {
		this.customerTransactionValidationTopicName = customerTransactionValidationTopicName;
	}

	public String getNameNode() {
		return nameNode;
	}

	public void setNameNode(String nameNode) {
		this.nameNode = nameNode;
	}

	public String getHivePath() {
		return hivePath;
	}

	public void setHivePath(String hivePath) {
		this.hivePath = hivePath;
	}

	public String getAtlasHost() {
		return atlasHost;
	}

	public void setAtlasHost(String atlasHost) {
		this.atlasHost = atlasHost;
	}

	public String getAtlasPort() {
		return atlasPort;
	}

	public void setAtlasPort(String atlasPort) {
		this.atlasPort = atlasPort;
	}

	public String getHiveMetaStoreURI() {
		return hiveMetaStoreURI;
	}

	public void setHiveMetaStoreURI(String hiveMetaStoreURI) {
		this.hiveMetaStoreURI = hiveMetaStoreURI;
	}

	public String getHiveDbName() {
		return hiveDbName;
	}

	public void setHiveDbName(String hiveDbName) {
		this.hiveDbName = hiveDbName;
	}
}
