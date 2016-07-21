package com.hortonworks.iot.financial.bolts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hortonworks.iot.financial.events.IncomingTransaction;
import com.hortonworks.iot.financial.util.StormProvenanceEvent;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/*
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
*/

public class InstantiateProvenance extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private String componentId;
	private String componentType;
	private OutputCollector collector;
	
	public void execute(Tuple tuple) {
		String transactionKey;
		
		if(tuple.getValueByField("TransactionKey") != null){
			transactionKey = (String) tuple.getValueByField("TransactionKey");
		}else{
			transactionKey = "";
		}
		IncomingTransaction incomingTransaction = (IncomingTransaction) tuple.getValueByField("IncomingTransaction");
		System.out.println("********************** Initializing Provenance for event: " + transactionKey);
		
		String actionType = "RECEIVE";
		List<StormProvenanceEvent> stormProvenance = new ArrayList<StormProvenanceEvent>();
	    StormProvenanceEvent provenanceEvent = new StormProvenanceEvent(transactionKey, actionType, tuple.getSourceComponent(), "SPOUT");
	    stormProvenance.add(provenanceEvent);
	    actionType = "MODIFY";
	    provenanceEvent = new StormProvenanceEvent(transactionKey, actionType, componentId, componentType);
	    stormProvenance.add(provenanceEvent);
			
		collector.emit(tuple, new Values((IncomingTransaction)incomingTransaction, stormProvenance));
		collector.ack(tuple);
	}

	public void prepare(Map map, TopologyContext context, OutputCollector collector) {
		this.componentId = context.getThisComponentId();
		this.componentType = "BOLT";
		this.collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("IncomingTransaction","ProvenanceEvent"));
	}
}