package com.hortonworks.iot.financial.bolts;

import java.util.HashMap;
import java.util.Map;

import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;

import com.hortonworks.iot.financial.events.EnrichedTransaction;
import com.hortonworks.iot.financial.util.Constants;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class PublishTransaction extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private String pubSubUrl = Constants.pubSubUrl;
	private String incomingTransactionsChannel = Constants.incomingTransactionsChannel;
	private BayeuxClient bayuexClient;
	private OutputCollector collector;
	
	public void execute(Tuple tuple) {
EnrichedTransaction transaction = (EnrichedTransaction) tuple.getValueByField("EnrichedTransaction");
		
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("transactionId", transaction.getTransactionId());
		data.put("transactionTimeStamp", transaction.getTransactionTimeStamp());
		data.put("accountNumber", transaction.getAccountNumber());
		data.put("accountType", transaction.getAccountType());
		data.put("latitude", transaction.getLatitude());
		data.put("longitude", transaction.getLongitude());
		data.put("amount", Integer.valueOf(transaction.getAmount()));
		data.put("merchantId", transaction.getMerchantId());
		data.put("merchantType", transaction.getMerchantType());
		
		switch (transaction.getMerchantType()) {
		case "electronics_store":
			data.put("amountMean", transaction.getElecAmtMean());
			data.put("amountDev", transaction.getElecAmtDev());
			break;
		case "health_beauty":
			data.put("amountMean", transaction.getHbAmtMean());
			data.put("amountDev", transaction.getHbAmtDev());
			break;
		case "entertainment":
			data.put("amountMean", transaction.getEntAmtMean());
			data.put("amountDev", transaction.getEntAmtDev());
			break;
		case "grocery_or_supermarket":
			data.put("amountMean", transaction.getGrocAmtMean());
			data.put("amountDev", transaction.getGrocAmtDev());
			break;
		case "convenience_store":
			data.put("amountMean", transaction.getConAmtMean());
			data.put("amountDev", transaction.getConAmtDev());
			break;
		case "clothing_store":
			data.put("amountMean", transaction.getrAmtMean());
			data.put("amountDev", transaction.getrAmtDev());
			break;
		case "restaurant":
			data.put("amountMean", transaction.getRestAmtMean());
			data.put("amountDev", transaction.getRestAmtDev());			
			break;
		case "gas_station":
			data.put("amountMean", transaction.getGasAmtMean());
			data.put("amountDev", transaction.getGasAmtDev());
			break;
		case "bar":
			data.put("amountMean", transaction.getRestAmtMean());
			data.put("amountDev", transaction.getRestAmtDev());
			break;
		}
		data.put("fraudulent", transaction.getFraudulent());
		data.put("distanceMean", transaction.getDistanceMean());
		data.put("distanceDev", transaction.getDistanceDev());
		data.put("distanceHome", transaction.getDistanceFromHome());
		data.put("distancePrev", transaction.getDistanceFromPrev());
		data.put("score", transaction.getScore());
		
		System.out.println(data.get("amountMean"));
		System.out.println(data.get("amountDev"));
		System.out.println(transaction.getDistanceMean());
		System.out.println(transaction.getDistanceDev());
		System.out.println(transaction.getDistanceFromHome());
		System.out.println(transaction.getDistanceFromPrev());
		bayuexClient.getChannel(incomingTransactionsChannel).publish(data);
		
		collector.emit(tuple, new Values((EnrichedTransaction)transaction));
		collector.ack(tuple);
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.collector = collector;
		
		HttpClient httpClient = new HttpClient();
		try {
			httpClient.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Prepare the transport
		Map<String, Object> options = new HashMap<String, Object>();
		ClientTransport transport = new LongPollingTransport(options, httpClient);

		// Create the BayeuxClient
		bayuexClient = new BayeuxClient(pubSubUrl, transport);
		
		bayuexClient.handshake();
		boolean handshaken = bayuexClient.waitFor(3000, BayeuxClient.State.CONNECTED);
		if (handshaken)
		{
			System.out.println("Connected to Cometd Http PubSub Platform");
		}
		else{
			System.out.println("Could not connect to Cometd Http PubSub Platform");
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("EnrichedTransaction"));
	}
}