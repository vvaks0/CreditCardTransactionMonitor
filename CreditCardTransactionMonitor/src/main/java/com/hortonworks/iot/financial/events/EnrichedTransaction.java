package com.hortonworks.iot.financial.events;

import java.io.Serializable;

public class EnrichedTransaction extends IncomingTransaction implements Serializable{
	private static final long serialVersionUID = 1L;

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
	private String fraudulent = "false";
	private String source;
	private Integer score = 0;
	
	private double rAmtDev;
	private double rAmtMean;
	private double elecAmtDev;
	private double elecAmtMean;
	private double grocAmtDev;
	private double grocAmtMean;
	private double gasAmtDev;
	private double gasAmtMean;
	private double entAmtDev;
	private double entAmtMean;
	private double conAmtMean;
	private double conAmtDev;
	private double hbAmtMean;
	private double hbAmtDev;
	private double restAmtMean;
	private double restAmtDev;
	private double distanceDev;
	private double distanceMean;
	private double timeDeltaSecDev;
	private double timeDetlaSecMean;
	private double distanceFromHome;
	private double distanceFromPrev;
	
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
	public Integer getScore() {
		return score;
	}
	public void setScore(Integer score) {
		this.score = score;
	}
	public double getrAmtDev() {
		return rAmtDev;
	}
	public void setrAmtDev(double rAmtDev) {
		this.rAmtDev = rAmtDev;
	}
	public double getrAmtMean() {
		return rAmtMean;
	}
	public void setrAmtMean(double rAmtMean) {
		this.rAmtMean = rAmtMean;
	}
	public double getElecAmtDev() {
		return elecAmtDev;
	}
	public void setElecAmtDev(double elecAmtDev) {
		this.elecAmtDev = elecAmtDev;
	}
	public double getElecAmtMean() {
		return elecAmtMean;
	}
	public void setElecAmtMean(double elecAmtMean) {
		this.elecAmtMean = elecAmtMean;
	}
	public double getGrocAmtDev() {
		return grocAmtDev;
	}
	public void setGrocAmtDev(double grocAmtDev) {
		this.grocAmtDev = grocAmtDev;
	}
	public double getGrocAmtMean() {
		return grocAmtMean;
	}
	public void setGrocAmtMean(double grocAmtMean) {
		this.grocAmtMean = grocAmtMean;
	}
	public double getGasAmtDev() {
		return gasAmtDev;
	}
	public void setGasAmtDev(double gasAmtDev) {
		this.gasAmtDev = gasAmtDev;
	}
	public double getGasAmtMean() {
		return gasAmtMean;
	}
	public void setGasAmtMean(double gasAmtMean) {
		this.gasAmtMean = gasAmtMean;
	}
	public double getEntAmtDev() {
		return entAmtDev;
	}
	public void setEntAmtDev(double entAmtDev) {
		this.entAmtDev = entAmtDev;
	}
	public double getEntAmtMean() {
		return entAmtMean;
	}
	public void setEntAmtMean(double entAmtMean) {
		this.entAmtMean = entAmtMean;
	}
	public double getConAmtMean() {
		return conAmtMean;
	}
	public void setConAmtMean(double conAmtMean) {
		this.conAmtMean = conAmtMean;
	}
	public double getConAmtDev() {
		return conAmtDev;
	}
	public void setConAmtDev(double conAmtDev) {
		this.conAmtDev = conAmtDev;
	}
	public double getHbAmtMean() {
		return hbAmtMean;
	}
	public void setHbAmtMean(double hbAmtMean) {
		this.hbAmtMean = hbAmtMean;
	}
	public double getRestAmtMean() {
		return restAmtMean;
	}
	public void setRestAmtMean(double restAmtMean) {
		this.restAmtMean = restAmtMean;
	}
	public double getHbAmtDev() {
		return hbAmtDev;
	}
	public void setHbAmtDev(double hbAmtDev) {
		this.hbAmtDev = hbAmtDev;
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
	public double getDistanceFromHome() {
		return distanceFromHome;
	}
	public void setDistanceFromHome(double distanceFromHome) {
		this.distanceFromHome = distanceFromHome;
	}
	public double getDistanceFromPrev() {
		return distanceFromPrev;
	}
	public void setDistanceFromPrev(double distanceFromPrev) {
		this.distanceFromPrev = distanceFromPrev;
	}
	
}
