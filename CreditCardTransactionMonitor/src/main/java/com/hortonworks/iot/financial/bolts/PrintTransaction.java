package com.hortonworks.iot.financial.bolts;

import java.util.Map;

import com.hortonworks.iot.financial.events.EnrichedTransaction;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class PrintTransaction extends BaseRichBolt {
	private OutputCollector collector;

	public void execute(Tuple tuple) {
		EnrichedTransaction transaction = (EnrichedTransaction) tuple.getValueByField("EnrichedTransaction");
		System.out.println(transaction.toString());	
		collector.ack(tuple);
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.collector = collector;	
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("EnrichedTransaction"));
	}

}
