package com.hortonworks.iot.financial;

public class AccountDetails {
	private String accountNumber;
	private String accountType;
	private String isAccountActive;
	private String expMonth;
	private String expYear;
	private String firstName;
	private String lastName;
	private String age;
	private String gender;
	private String streetAddress;
	private String city;
	private String state;
	private String zipCode;
	private String homeLatitude;
	private String homeLongitude;
	
	private double retAmountDev;
	private double retAmountMean;
	private double elecAmountDev;
	private double elecAmountMean;
	private double grocAmountDev;
	private double grocAmountMean;
	private double gasAmountDev;
	private double gasAmountMean;
	private double entAmountDev;
	private double entAmountMean;
	private double conAmountMean;
	private double conAmountDev;
	private double hbAmountMean;
	private double hbAmountDev;
	private double restAmtMean;
	private double restAmtDev;
	private double distanceDev;
	private double distanceMean;
	private double timeDeltaSecDev;
	private double timeDetlaSecMean;
	
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getStreetAddress() {
		return streetAddress;
	}
	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}
	public String getAge() {
		return age;
	}
	public void setAge(String age) {
		this.age = age;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getZipCode() {
		return zipCode;
	}
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}
	public String getHomeLatitude() {
		return homeLatitude;
	}
	public void setHomeLatitude(String homeLatitude) {
		this.homeLatitude = homeLatitude;
	}
	public String getHomeLongitude() {
		return homeLongitude;
	}
	public void setHomeLongitude(String homeLongitude) {
		this.homeLongitude = homeLongitude;
	}
	public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getAccountNumber() {
		return accountNumber;
	}
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
	public String getAccountType() {
		return accountType;
	}
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
	public String getExpMonth() {
		return expMonth;
	}
	public void setExpMonth(String expMonth) {
		this.expMonth = expMonth;
	}
	public String getExpYear() {
		return expYear;
	}
	public void setExpYear(String expYear) {
		this.expYear = expYear;
	}
	public String getIsAccountActive() {
		return isAccountActive;
	}
	public void setIsAccountActive(String isAccountActive) {
		this.isAccountActive = isAccountActive;
	}
	public double getRetAmountDev() {
		return retAmountDev;
	}
	public void setRetAmountDev(double retAmountDev) {
		this.retAmountDev = retAmountDev;
	}
	public double getRetAmountMean() {
		return retAmountMean;
	}
	public void setRetAmountMean(double retAmountMean) {
		this.retAmountMean = retAmountMean;
	}
	public double getElecAmountDev() {
		return elecAmountDev;
	}
	public void setElecAmountDev(double elecAmountDev) {
		this.elecAmountDev = elecAmountDev;
	}
	public double getElecAmountMean() {
		return elecAmountMean;
	}
	public void setElecAmountMean(double elecAmountMean) {
		this.elecAmountMean = elecAmountMean;
	}
	public double getGrocAmountDev() {
		return grocAmountDev;
	}
	public void setGrocAmountDev(double grocAmountDev) {
		this.grocAmountDev = grocAmountDev;
	}
	public double getGrocAmountMean() {
		return grocAmountMean;
	}
	public void setGrocAmountMean(double grocAmountMean) {
		this.grocAmountMean = grocAmountMean;
	}
	public double getGasAmountDev() {
		return gasAmountDev;
	}
	public void setGasAmountDev(double gasAmountDev) {
		this.gasAmountDev = gasAmountDev;
	}
	public double getGasAmountMean() {
		return gasAmountMean;
	}
	public void setGasAmountMean(double gasAmountMean) {
		this.gasAmountMean = gasAmountMean;
	}
	public double getEntAmountDev() {
		return entAmountDev;
	}
	public void setEntAmountDev(double entAmountDev) {
		this.entAmountDev = entAmountDev;
	}
	public double getEntAmountMean() {
		return entAmountMean;
	}
	public void setEntAmountMean(double entAmountMean) {
		this.entAmountMean = entAmountMean;
	}
	public double getConAmountMean() {
		return conAmountMean;
	}
	public void setConAmountMean(double conAmountMean) {
		this.conAmountMean = conAmountMean;
	}
	public double getConAmountDev() {
		return conAmountDev;
	}
	public void setConAmountDev(double conAmountDev) {
		this.conAmountDev = conAmountDev;
	}
	public double getHbAmountMean() {
		return hbAmountMean;
	}
	public void setHbAmountMean(double hbAmountMean) {
		this.hbAmountMean = hbAmountMean;
	}
	public double getHbAmountDev() {
		return hbAmountDev;
	}
	public void setHbAmountDev(double hbAmountDev) {
		this.hbAmountDev = hbAmountDev;
	}
	public double getRestAmtMean() {
		return restAmtMean;
	}
	public void setRestAmtMean(double restAmtMean) {
		this.restAmtMean = restAmtMean;
	}
	public double getRestAmtDev() {
		return restAmtDev;
	}
	public void setRestAmtDev(double restAmtDev) {
		this.restAmtDev = restAmtDev;
	}
	public double getDistanceDev() {
		return distanceDev;
	}
	public void setDistanceDev(double distanceDev) {
		this.distanceDev = distanceDev;
	}
	public double getDistanceMean() {
		return distanceMean;
	}
	public void setDistanceMean(double distanceMean) {
		this.distanceMean = distanceMean;
	}
	public double getTimeDeltaSecDev() {
		return timeDeltaSecDev;
	}
	public void setTimeDeltaSecDev(double timeDeltaSecDev) {
		this.timeDeltaSecDev = timeDeltaSecDev;
	}
	public double getTimeDetlaSecMean() {
		return timeDetlaSecMean;
	}
	public void setTimeDetlaSecMean(double timeDetlaSecMean) {
		this.timeDetlaSecMean = timeDetlaSecMean;
	}
	
}