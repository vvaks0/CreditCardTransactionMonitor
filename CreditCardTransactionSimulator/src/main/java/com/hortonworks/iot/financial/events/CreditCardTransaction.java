package com.hortonworks.iot.financial.events;

public class CreditCardTransaction {
	private String accountNumber;
	private String accountType;
	private String merchantId;
	private String merchantType;
	private String transactionId;
	private String amount;
	private String currency;
	private String isCardPresent;
	private String latitude;
	private String longitude;
	private String ipAddress;
	private String transactionTimeStamp;
	
	public String getAccountNumber(){
		return accountNumber;
	}
	public String getAccountType() {
		return accountType;
	}
	public String getMerchantId(){
		return merchantId;
	}
	public String getMerchantType(){
		return merchantType;
	}
	public String getTransactionId(){
		return transactionId;
	}
	public String getAmount(){
		return amount;
	}
	public String getCurrency(){
		return currency;
	}
	public String getIsCardPresent(){
		return isCardPresent;
	}
	public String getLatitude(){
		return latitude;
	}
	public String getLongitude(){
		return longitude;
	}
	public String getIpAddress(){
		return ipAddress;
	}
	public void setAccountNumber(String value){
		accountNumber = value;
	}
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
	public void setMerchantId(String value){
		merchantId = value;
	}
	public void setMerchantType(String value){
		merchantType = value;
	}
	public void setTransactionId(String value){
		transactionId = value;
	}
	public void setAmount(String value){
		amount = value;
	}
	public void setCurrency(String value){
		currency = value;
	}
	public void setIsCardPresent(String value){
		isCardPresent = value;
	}
	public void setLatitude(String value){
		latitude = value;
	}
	public void setLongitude(String value){
		longitude = value;
	}
	public void setIpAddress(String value){
		ipAddress = value;
	}
	public String getTransactionTimeStamp() {
		return transactionTimeStamp;
	}
	public void setTransactionTimeStamp(String transactionTimeStamp) {
		this.transactionTimeStamp = transactionTimeStamp;
	}
}
