package com.hortonworks.iot.financial.rest;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

@Path("CreditCardSimulatorService")
public class CreditCardSimulatorService {
		static ThreadGroup rootThreadGroup = null;

		@POST
		@Produces(MediaType.TEXT_PLAIN)
		@Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.WILDCARD})
		public Response routeTechnicians(String data) {
			String result = "Data post: " + data;
			System.out.println(result);
			//TechnicianDestination techDestination = (TechnicianDestination)convertJSONToPOJO(data);
			
			CacheManager cacheManager = CacheManager.getInstance();
			Cache cache = cacheManager.getCache("CustomerActionRequest");
			//Element element = new Element(techDestination.getTechnicianId(), techDestination);
			//cache.put(element);
			
			return Response.status(200).entity(result).build();
			//System.out.println(technicianId);
			//Thread controlThread = getThread("Device: " + technicianId);
			//System.out.println("Got handle to thread :" + controlThread.getName());
			//return Response.ok("Success" , MediaType.TEXT_PLAIN).build(); 
		}
		
		public Object convertJSONToPOJO(String jsonString) {
	    	ObjectMapper mapper = new ObjectMapper();
	    	Object pojo = null;
	    	try {
	    		pojo = mapper.readValue(jsonString, Object.class);
	    	} catch (JsonGenerationException e) {
	    		e.printStackTrace();
	    	} catch (JsonMappingException e) {
	    		e.printStackTrace();
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    	return pojo;
	    }

		private static Thread getThread(final String name) {
			if ( name == null )
				throw new NullPointerException( "Null name" );
			final Thread[] threads = getGroupThreads(getRootThreadGroup( ));
			for ( Thread thread : threads )
				if ( thread.getName( ).equals( name ) )
					return thread;
			return null;
		}
	 
		private static ThreadGroup getRootThreadGroup( ) {
			if ( rootThreadGroup != null )
				return rootThreadGroup;
			ThreadGroup tg = Thread.currentThread( ).getThreadGroup( );
			ThreadGroup ptg;
			while ( (ptg = tg.getParent( )) != null )
				tg = ptg;
			return tg;
		}

		private static Thread[] getGroupThreads(final ThreadGroup group ) {
			if ( group == null )
				throw new NullPointerException( "Null thread group" );
			
			int nAlloc = group.activeCount( );
			int n = 0;
			Thread[] threads;
			do {
				nAlloc *= 2;
				threads = new Thread[ nAlloc ];
				n = group.enumerate( threads );
			} while ( n == nAlloc );
			return java.util.Arrays.copyOf( threads, n );
		}
}
