package com.hortonworks.iot.financial.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.iot.financial.events.CustomerResponse;

import org.apache.storm.kafka.StringScheme;

/*
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.spout.Scheme;
*/

import org.apache.storm.spout.Scheme;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;


public class CustomerResponseEventJSONScheme implements Scheme  {
		private static final long serialVersionUID = 1L;
		private static final Charset UTF8 = Charset.forName("UTF-8");
		
		@Override
		public List<Object> deserialize(ByteBuffer value) {
			String eventJSONString = StringScheme.deserializeString(value);
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
		
		public List<Object> deserialize(byte[] value) {
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
}