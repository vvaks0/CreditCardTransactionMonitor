package com.hortonworks.iot.financial.spout;

import java.util.Map;

import com.hortonworks.iot.financial.events.IncomingTransaction;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class KafkaSpout extends BaseRichSpout {
	SpoutOutputCollector collector;
	@Override
	public void nextTuple() {
		collector.emit(new Values(new IncomingTransaction()));
	}

	@Override
	public void open(Map arg0, TopologyContext arg1, SpoutOutputCollector collector) {
		this.collector = collector;
		
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("IncomingTransaction"));
		
	}

}
