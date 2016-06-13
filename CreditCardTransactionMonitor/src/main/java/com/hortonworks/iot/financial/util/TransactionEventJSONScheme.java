package com.hortonworks.iot.financial.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.iot.financial.events.IncomingTransaction;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import storm.kafka.KeyValueScheme;

public class TransactionEventJSONScheme implements KeyValueScheme {
		private static final long serialVersionUID = 1L;
		private static final Charset UTF8 = Charset.forName("UTF-8");

		public List<Object> deserializeKeyAndValue(byte[] key, byte[] value) {
			String eventKey = new String(key, UTF8);
			String eventJSONString = new String(value, UTF8);
	        IncomingTransaction incomingTransaction = null;
	        ObjectMapper mapper = new ObjectMapper();
	        
	        try {
				incomingTransaction = mapper.readValue(eventJSONString, IncomingTransaction.class);
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	        return new Values(eventKey, incomingTransaction);
	    }

	    public Fields getOutputFields() {
	        return new Fields("TransactionKey", "IncomingTransaction");
	    }

		@Override
		public List<Object> deserialize(byte[] arg0) {
			return null;
		}
}