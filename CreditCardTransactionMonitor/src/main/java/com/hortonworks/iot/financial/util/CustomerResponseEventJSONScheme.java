package com.hortonworks.iot.financial.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.iot.financial.events.CustomerResponse;
import com.hortonworks.iot.financial.events.IncomingTransaction;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import storm.kafka.KeyValueScheme;

/*
import org.apache.storm.spout.Scheme;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
*/

public class CustomerResponseEventJSONScheme implements KeyValueScheme  {
		private static final long serialVersionUID = 1L;
		private static final Charset UTF8 = Charset.forName("UTF-8");

	    public List<Object> deserialize(byte[] bytes) {
	        String eventJSONString = new String(bytes, UTF8);
	        CustomerResponse customerResponse = null;
	        ObjectMapper mapper = new ObjectMapper();
	        
	        try {
				customerResponse = mapper.readValue(eventJSONString, CustomerResponse.class);
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	        return new Values(customerResponse);
	    }

	    public Fields getOutputFields() {
	        return new Fields("CustomerResponse");
	    }

		@Override
		public List<Object> deserializeKeyAndValue(byte[] key, byte[] value) {
			String keyString = new String(key, UTF8); 
			String eventJSONString = new String(value, UTF8);
		        CustomerResponse customerResponse = null;
		        ObjectMapper mapper = new ObjectMapper();
		        
		        try {
					customerResponse = mapper.readValue(eventJSONString, CustomerResponse.class);
				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		        return new Values(customerResponse);

		}
		
		public List<Object> deserializeKeyAndValue(ByteBuffer arg0, ByteBuffer arg1) {
			// TODO Auto-generated method stub
			return null;
		}
}