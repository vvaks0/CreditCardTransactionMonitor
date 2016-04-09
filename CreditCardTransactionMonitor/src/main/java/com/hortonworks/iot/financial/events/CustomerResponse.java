package com.hortonworks.iot.financial.events;

import java.io.Serializable;

public class CustomerResponse implements Serializable{
	private static final long serialVersionUID = 1L;

	private String accountNumber;
	private String transactionId;
	//private String transactionTimeStamp;
	private String fraudulent;
	private String source;
	
	public String getAccountNumber() {
		return accountNumber;
	}
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	/*
	public String getTransactionTimeStamp() {
		return transactionTimeStamp;
	}
	public void setTransactionTimeStamp(String transactionTimeStamp) {
		this.transactionTimeStamp = transactionTimeStamp;
	}*/
	public String getFraudulent() {
		return fraudulent;
	}
	public void setFraudulent(String fraudulent) {
		this.fraudulent = fraudulent;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
}
