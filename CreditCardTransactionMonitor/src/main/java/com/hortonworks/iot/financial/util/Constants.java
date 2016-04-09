package com.hortonworks.iot.financial.util;

public class Constants {
	public static final String zkHost = "sandbox.hortonworks.com";
	public static final String zkPort = "2181";
	public static final String zkConnString = zkHost+":"+zkPort;
	public static final String incomingTransactionsTopicName = "IncomingTransactions";
	public static final String customerTransactionValidationTopicName = "CustomerTransactionValidation";
	
	public static final String pubSubUrl = "http://sandbox.hortonworks.com:8091/cometd";
	public static final String incomingTransactionsChannel = "/incomingTransactions";
	public static final String fraudAlertChannel = "/fraudAlert";
	public static final String accountStatusUpdateChannel = "/accountStatusUpdate";
	
	public static final String nameNode = "hdfs://sandbox.hortonworks.com:8020";
	public static final String hivePath = "/demo/data/transaction_logs";
}
