package com.hortonworks.iot.financial.types;

import com.google.appengine.api.search.GeoPoint;

public class Merchant {
	private String merchantId;
	private String merchantType;
	private GeoPoint location;
	
	public Merchant(){}
	public Merchant(String merchantId, String merchantType, GeoPoint location){
		this.merchantId = merchantId;
		this.merchantType = merchantType;
		this.location = location;
	}
	
	public String getMerchantId() {
		return merchantId;
	}
	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}
	public String getMerchantType() {
		return merchantType;
	}
	public void setMerchantType(String merchantType) {
		this.merchantType = merchantType;
	}
	public GeoPoint getLocation() {
		return location;
	}
	public void setLocation(GeoPoint location) {
		this.location = location;
	}
}
