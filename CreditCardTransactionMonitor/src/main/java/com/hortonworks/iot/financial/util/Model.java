package com.hortonworks.iot.financial.util;

import java.util.List;
import java.util.Vector;

import org.apache.spark.mllib.classification.SVMModel;
import org.apache.spark.mllib.linalg.Vectors;

import com.hortonworks.iot.financial.events.EnrichedTransaction;

public class Model {
	private SVMModel svmModel;
	
	public Model(SVMModel model){
		svmModel = model;
	}
	
	public EnrichedTransaction calculateFraudScore(EnrichedTransaction transaction, EnrichedTransaction previousTransaction) {
		/** init phase */
		// 1) read in profiles and cache, key acount number
		//HashMap<String, Profile> pMap = new HashMap<String, Profile>();

		// 2) read in last 3-4 transactions and cache, key account number
		//HashMap<String, List<EnrichedTransaction>> tMap = new HashMap<String, List<EnrichedTransaction>>();	
		
		// 3) STREAMING VALUES pass in HERE
		EnrichedTransaction curr = transaction;
		EnrichedTransaction prev = previousTransaction;
		// pass in values from stream here
		//pMap.get(curr.getAccountNumber());
		//List<EnrichedTransaction> tList = tMap.get(curr.getAccountNumber());
		
		// 4) Feature Engineering
		//EnrichedTransaction prev1 = tList.get(tList.size() - 1);
		//EnrichedTransaction prev2 = tList.get(tList.size() - 2);
		//EnrichedTransaction prev3 = tList.get(tList.size() - 3);
		
		// Category 
		//fx: (Math.abs(curr.getAmount() - p.getGROCAmountMean()) / p.getGROCAmountDev());
		double catNSD = 0.0;
		switch (curr.getMerchantType()) {

		case "electronics_store":
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - curr.getElecAmtMean()) / curr.getElecAmtDev());
			break;
		case "health_beauty":
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - curr.getHbAmtMean()) / curr.getHbAmtDev());
			break;
		case "entertainment":
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - curr.getEntAmtMean()) / curr.getEntAmtDev());
			break;
		case "grocery_or_supermarket":
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - curr.getGrocAmtMean()) / curr.getGrocAmtDev());
			break;
		case "convenience_store":
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - curr.getConAmtMean()) / curr.getConAmtDev());
			break;
		case "clothing_store":
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - curr.getrAmtMean()) / curr.getrAmtDev());
			break;
		case "restaurant":
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - curr.getRestAmtMean()) / curr.getRestAmtDev());
			break;
		case "gas_station":
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - curr.getGasAmtMean()) / curr.getGasAmtDev());
			break;
		case "bar":
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - curr.getRestAmtMean()) / curr.getRestAmtDev());
			break;
		default : 
			catNSD = (Math.abs(Integer.valueOf(curr.getAmount()) - 40.0) / 3.0); // ideally this would be the total mean
			break;
		}

		// Time 
		
		/* List<Double> windowDeltaTimes = new ArrayList<Double>();
		windowDeltaTimes.add((double) TimeUnit.MILLISECONDS
				.toSeconds(prev3.getTransactionTimeStamp().getTime() - prev2.getTransactionTimeStamp().getTime()));
		windowDeltaTimes.add((double) TimeUnit.MILLISECONDS
				.toSeconds(prev2.getTransactionTimeStamp().getTime() - prev1.getTransactionTimeStamp().getTime()));
		windowDeltaTimes.add((double) TimeUnit.MILLISECONDS
				.toSeconds(curr.getTransactionTimeStamp().getTime() - prev1.getTransactionTimeStamp().getTime()));
		double recentPurchaseTimeDev = deviation(windowDeltaTimes);

		double max = 0.0;
		double min = 0.0;
		if (recentPurchaseTimeDev > p.getTimeDeltaSecDev()) {
			max = recentPurchaseTimeDev;
			min = p.getTimeDeltaSecDev();
		} else {
			max = p.getTimeDeltaSecDev();
			min = recentPurchaseTimeDev;
		}

		double timeNSD = (max / min); */
		// Distance
		double distance = distance(
				Double.valueOf(curr.getLatitude()), 
				Double.valueOf(curr.getLongitude()),
				Double.valueOf(prev.getLatitude()), 
				Double.valueOf(prev.getLongitude()));
				
		double distanceHome = distance(
				Double.valueOf(curr.getLatitude()), 
				Double.valueOf(curr.getLongitude()), 
				Double.valueOf(curr.getHomeLatitude()), 
				Double.valueOf(curr.getHomeLongitude()));
		
		double distanceNSD = (Math.abs(distance - curr.getDistanceMean()) / curr.getDistanceDev());

		curr.setDistanceFromHome(distanceHome);
		curr.setDistanceFromPrev(distance);
		
		// Features
		// curr.accountNumber, catNSD timeNSD distanceNSD
		Vector<Double> v = new Vector<Double>();
		v.add(catNSD);
		//v.add(timeNSD);
		v.add(distanceNSD);
		double[] vector = {catNSD, 10, distanceNSD};
		
		// call model here, compiled from spark
		double probability = svmModel.predict(Vectors.dense(vector));
		// model output in terms of probability
		System.out.println("*******************Prediction Fraud Probability: " + probability);
		
		// remember that a .49 means it is predicting that is is NOT fraud. so a 55 is not a 55 score, it is at least twice as high.  
		// the model seems to mostly predict small numbers like .52 and .58 because it hasn't seen enough fraud to be confident which is
		// why in this case we will have to manually create a score, normally we wouldn't but makes sense here. 
		int score = 0;
		if ((probability*100) < 50) {
			score = 0; // green
		}
		else if ((probability*100) > 50 && (probability*100) <= 65) {
			score = 50; // yellow
		}
		else if ((probability*100) > 65 && (probability*100) <=75) {
			score = 75; // amber
		}
		else if ((probability*100) > 75){
			score = 100; // red
		}
		
		curr.setScore(score);
		return curr;
	}

	public double variance(List<Double> elems) {
		double tmp = 0.0;
		double mean = sum(elems) / elems.size();
		for (double e : elems) {
			tmp += Math.pow((e - mean), 2);
		}
		return tmp / elems.size();
	}

	public double deviation(List<Double> elems) {
		return Math.sqrt(variance(elems));
	}

	public static double sum(List<Double> elems) {
		double sum = 0.0;
		for (double e : elems) {
			sum += e;
		}
		return sum;
	}

	public static double mean(List<Double> elems) {
		double sum = 0.0;
		for (double e : elems) {
			sum += e;
		}
		return sum / elems.size();
	}

	public static double distance(double lat1, double lng1, double lat2, double lng2) {
		double x1 = Math.toRadians(lat1);
		double y1 = Math.toRadians(lng1);
		double x2 = Math.toRadians(lat2);
		double y2 = Math.toRadians(lng2);

		double dlon = y2 - y1;
		double dlat = x2 - x1;

		double a = Math.pow((Math.sin(dlat / 2)), 2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return 3958.75 * c;
	}

	/*
	 * def median(elems: List[Double]): Double = {
	 * 
	 * elems.toArray.sortWith(_) if (elems.length % 2 == 0) { return
	 * (elems((elems.length / 2) - 1) + elems(elems.length / 2)) / 2.0; } else {
	 * return elems(elems.length / 2); } }
	 */

}
