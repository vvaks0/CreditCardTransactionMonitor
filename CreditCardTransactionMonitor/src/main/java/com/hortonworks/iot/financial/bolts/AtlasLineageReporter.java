package com.hortonworks.iot.financial.bolts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.json.InstanceSerialization;
import org.apache.atlas.typesystem.TypesDef;
import org.apache.atlas.typesystem.json.TypesSerialization;
import org.apache.atlas.typesystem.persistence.Id;
import org.apache.atlas.typesystem.types.AttributeDefinition;
import org.apache.atlas.typesystem.types.ClassType;
import org.apache.atlas.typesystem.types.EnumTypeDefinition;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.apache.atlas.typesystem.types.Multiplicity;
import org.apache.atlas.typesystem.types.StructTypeDefinition;
import org.apache.atlas.typesystem.types.TraitType;
import org.apache.atlas.typesystem.types.utils.TypesUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.hortonworks.iot.financial.util.Constants;
import com.hortonworks.iot.financial.util.StormProvenanceEvent;

import org.apache.commons.codec.binary.Base64;

import backtype.storm.generated.SpoutSpec;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.utils.Utils;

/*
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.apache.storm.generated.SpoutSpec;
*/

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class AtlasLineageReporter extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private boolean skipReport = false;
	private String DEFAULT_APP_PORT_STR = "21000";
	private String DEFAULT_ATLAS_REST_ADDRESS = "http://sandbox.hortonworks.com:21000";
	private String DEFAULT_ADMIN_USER = "admin";
	private String DEFAULT_ADMIN_PASS = "admin";
	private String atlasUrl = DEFAULT_ATLAS_REST_ADDRESS;
	private String atlasPasswordEncoding = "YWRtaW46YWRtaW4=";
	private String stormTopologyName;
	private Double atlasVersion;
	private AtlasClient atlasClient;
	private OutputCollector collector;
	private Referenceable topology;
	private Constants constants;
	private Map topologyConf;
	private Map<String,SpoutSpec> spouts;
	private Map<String, EnumTypeDefinition> enumTypeDefinitionMap = new HashMap<String, EnumTypeDefinition>();
	private Map<String, StructTypeDefinition> structTypeDefinitionMap = new HashMap<String, StructTypeDefinition>();
	private Map<String, HierarchicalTypeDefinition<ClassType>> classTypeDefinitions = new HashMap<String, HierarchicalTypeDefinition<ClassType>>();
	
	@SuppressWarnings({ "unused", "unchecked" })
	public void execute(Tuple tuple) {
		List<String> lineage = new ArrayList<String>();
		List<StormProvenanceEvent> stormProvenance = (List<StormProvenanceEvent>)tuple.getValueByField("ProvenanceEvent");
		Map<String, String> spoutValueMap = new HashMap<String, String>();
		String transactionKey;
		String spoutName;
		Serializable spoutInstance;
		Referenceable incomingEvent = null;
		Referenceable outgoingEvent = null;
		String generatedUuid = null;
		if(!skipReport){
			Iterator<StormProvenanceEvent> iterator = stormProvenance.iterator();
			StormProvenanceEvent currentEvent = new StormProvenanceEvent();
			while(iterator.hasNext()){
				currentEvent = iterator.next();
				transactionKey = currentEvent.getEventKey();
				System.out.println("********************* Printing Lineage for Event: " + transactionKey);
				System.out.println("********************* Component Name: " + currentEvent.getComponentName());
				System.out.println("********************* Component Type: " + currentEvent.getComponentType());  
				if(currentEvent.getComponentType().equalsIgnoreCase("SPOUT")){
					spoutName =  currentEvent.getComponentName();
					spoutInstance = Utils.javaDeserialize(spouts.get(spoutName).get_spout_object().get_serialized_java(), Serializable.class);
					if(spoutInstance.getClass().getSimpleName().equalsIgnoreCase("KafkaSpout")){
						try {
							spoutValueMap = getFieldValues(spoutInstance, false);
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
						System.out.println("********************* Source: " + spoutValueMap.get("_spoutConfig.topic"));
						System.out.println("********************* Source Type: " + spoutInstance.getClass().getSimpleName());
					}
					//System.out.println("********************* Entry Spout: " + spoutValueMap.toString());
					try {
						incomingEvent = getEventReference(currentEvent);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if(incomingEvent != null){
						System.out.println("********************* Source Referenceable Event: " + incomingEvent.toString());
					}else{
						System.out.println("********************* Could not find Referenceable Event, creating ....");
						try {
							incomingEvent = register(atlasClient, createEvent(currentEvent));
						} catch (Exception e) {
							e.printStackTrace();
						}
						System.out.println("********************* Source Referenceable Event: " + incomingEvent.toString());
					}
				}
				System.out.println("********************* Event Type: " + currentEvent.getEventType());
				if(currentEvent.getEventType().equalsIgnoreCase("SEND")){	
					System.out.println("********************* Destination: " + currentEvent.getTargetDataRepositoryLocation());
					System.out.println("********************* Destination Type: " + currentEvent.getTargetDataRepositoryType());
					try {
						outgoingEvent = null;//getEventReference(currentEvent);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if(outgoingEvent != null){
						System.out.println("********************* Destination Referenceable Event: " + outgoingEvent.toString());
					}else{
						System.out.println("********************* Could not find Referenceable Event, creating ....");
						generatedUuid = UUID.randomUUID().toString();
						try {
							outgoingEvent = register(atlasClient, createEvent(currentEvent, generatedUuid));
						} catch (Exception e) {
							e.printStackTrace();
					}
						System.out.println("********************* Destination Referenceable Event: " + outgoingEvent.toString());
					}
				}
				lineage.add(currentEvent.getComponentName());
			}
			try {
				//Search Atlas for the Topology that the instance was created from
				Referenceable referenceTopology = getTopologyReference(stormTopologyName);
				topology = createTopologyInstance(topologyConf, referenceTopology, incomingEvent, outgoingEvent, lineage);
				System.out.println("********************* Processing Topology: " + topology.getValuesMap().get("qualifiedName").toString());
				System.out.println("********************* Processing Topology: " + topology.getValuesMap().get("inputs").toString());
				System.out.println("********************* Processing Topology: " + topology.getValuesMap().get("outputs").toString());
				register(atlasClient, topology);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("ProvenanceEvent"));
	}
	
	public void prepare(Map map, TopologyContext context, OutputCollector collector) {
		Properties props = System.getProperties();
        props.setProperty("atlas.conf", "/usr/hdp/current/atlas-client/conf");
		this.collector = collector;
		this.spouts = context.getRawTopology().get_spouts();	
		this.constants = new Constants();
		this.stormTopologyName = map.get("topology.name").toString();
		this.atlasUrl = "http://" + constants.getAtlasHost() + ":" + constants.getAtlasPort();
		String[] basicAuth = {DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASS};
		String[] atlasURL = {atlasUrl};
		
		this.atlasClient = new AtlasClient(atlasURL, basicAuth);
		this.topologyConf = map;
		try{
			this.atlasVersion = Double.valueOf(getAtlasVersion(atlasUrl + "/api/atlas/admin/version", basicAuth));
		}catch(Exception e){
			atlasVersion = null;
		}
		if(atlasVersion != null && Double.valueOf(atlasVersion) >= 0.7){
			try {
				atlasClient.getType("event");
				atlasClient.getType("storm_topology_instance");					
				System.out.println("******************* Storm Lineage Atlas Types already exists");
			}catch (AtlasServiceException e) {
				System.out.println("******************* Storm Lineage Atlas Types are not presemt... creating");
				try {
					atlasClient.createType(generateStormEventLineageDataModel());
					System.out.println("******************* Storm Lineage Atlas Types have been created");
				} catch (AtlasServiceException e1) {
					e1.printStackTrace();
				}
			}
		}else{
			System.out.println("********************* Atlas is not present or Atlas version is incompatible, skip lineage reporting");
			this.skipReport = true;
		}
	}
	
	private Referenceable getEventReference(StormProvenanceEvent event) throws Exception {
		String typeName = "event";
		String id = "SEND_" + event.getEventKey();
		 
		String dslQuery = String.format("%s where %s = '%s'", typeName, "qualifiedName", id);
		System.out.println("********************* Atlas Version is: " + atlasVersion);
		Referenceable eventReferenceable = null;
		
		if(atlasVersion >= 0.7)
			eventReferenceable = getEntityReferenceFromDSL6(atlasClient, typeName, dslQuery);
		else
			eventReferenceable = null;
		
		return eventReferenceable;
	}
	
	private Referenceable getTopologyReference(String topologyName) throws Exception {
		String typeName = "storm_topology";
		 
		String dslQuery = String.format("%s where %s = '%s'", typeName, "qualifiedName", topologyName);
		System.out.println("********************* Atlas Version is: " + atlasVersion);
		Referenceable eventReferenceable = null;
		
		if(atlasVersion >= 0.7)
			eventReferenceable = getEntityReferenceFromDSL6(atlasClient, typeName, dslQuery);
		else
			eventReferenceable = null;
		
		return eventReferenceable;
	}
	
	public Referenceable register(final AtlasClient atlasClient, final Referenceable referenceable) throws AtlasServiceException {
        if (referenceable == null) {
            return null;
        }

        String typeName = referenceable.getTypeName();
        System.out.println("creating instance of type " + typeName);

        String entityJSON = InstanceSerialization.toJson(referenceable, true);
        System.out.println("Submitting new entity " + referenceable.getTypeName() + ":" + entityJSON);
        List<String> guid = atlasClient.createEntity(entityJSON);
        System.out.println("created instance for type " + typeName + ", guid: " + guid); //client version 0.7

        return new Referenceable(guid.get(guid.size() - 1) , referenceable.getTypeName(), null);
    }
	
	private Referenceable createEvent(StormProvenanceEvent event) {
        String qualifiedName = "SEND_" + event.getEventKey();
    	Referenceable processor = new Referenceable("event");
        
        processor.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, qualifiedName);
        processor.set("name", qualifiedName);
        processor.set("flowFileId", event.getEventKey());
        processor.set("event_key", "accountNumber");
        processor.set("description", "storm event");
        return processor;
    }
    
    private Referenceable createEvent(StormProvenanceEvent event, String uuid) {
        String qualifiedName = "SEND_" + uuid;
    	Referenceable processor = new Referenceable("event");
        
        processor.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, qualifiedName);
        processor.set("name", qualifiedName);
        processor.set("flowFileId", uuid);
        processor.set("event_key", "accountNumber");
        processor.set("description", "storm event");
        return processor;
    }
    
    private Referenceable createTopologyInstance(Map stormConf, Referenceable topologyReference, Referenceable inputEvent, Referenceable outputEvent, List<String> lineage) {
    	Id topologyReferenceId = null;
    	List<Id> sourceList = new ArrayList<Id>();
        List<Id> targetList = new ArrayList<Id>();
        sourceList.add(inputEvent.getId());
        targetList.add(outputEvent.getId());
        
        if(topologyReference != null){
        	topologyReferenceId = topologyReference.getId();
        }
        
        String topologyName = stormTopologyName + "_" + inputEvent.getId()._getId();
    	Referenceable topologyReferenceable = new Referenceable("storm_topology_instance");
    	topologyReferenceable.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, topologyName);
    	topologyReferenceable.set("name", topologyName);
    	topologyReferenceable.set("inputs", sourceList);
    	topologyReferenceable.set("outputs", targetList);
    	topologyReferenceable.set("nodes", lineage.toString());
    	topologyReferenceable.set("topologyReference", topologyReferenceId);

    	return topologyReferenceable;
    }

	private Referenceable getEntityReferenceFromDSL6(final AtlasClient atlasClient, final String typeName, final String dslQuery)
	           throws Exception {
		System.out.println("****************************** Query String: " + dslQuery);
		JSONArray results = atlasClient.searchByDSL(dslQuery);
	    //JSONArray results = searchDSL(atlasUrl + "/api/atlas/discovery/search/dsl?query=", dslQuery);
	    System.out.println("****************************** Query Results Count: " + results.length());
	    if (results.length() == 0) {
	    	return null;
	    } else {
	    	String guid;
	    	JSONObject row = results.getJSONObject(0);
	    	if (row.has("$id$")) {
	    		guid = row.getJSONObject("$id$").getString("id");
	    	} else {
	    		guid = row.getJSONObject("_col_0").getString("id");
	    	}
	    	System.out.println("****************************** Resulting JSON Object: " + row.toString());
	    	System.out.println("****************************** Inputs to Referenceable: " + guid + " : " + typeName);
	    	return new Referenceable(guid, typeName, null);
	    }
	}
	
	private String getAtlasVersion(String uri, String[] basicAuth){
		System.out.println("************************ Getting Atlas Version from: " + uri);
		JSONObject json = null;
		String versionValue = null;
        try{
        	//json = readJsonFromUrl(uri);
        	json = readJSONFromUrlAuth(uri, basicAuth);
        	System.out.println("************************ Response from Atlas: " + json);
        	versionValue = json.getString("Version");
        } catch (Exception e) {
            e.printStackTrace();
        }
		return versionValue.substring(0,3);
	}
	
	public JSONArray searchDSL(String uri, String query){
		query = query.replaceAll(" ", "+");
        System.out.println("************************" + query);
        JSONObject json = null;
        JSONArray jsonArray = null;
        try{
        	json = readJsonFromUrl(uri+query);
        	jsonArray = json.getJSONArray("results");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonArray;
    }
	
	private void createStormTopologyInstanceType(){
		  final String typeName = "storm_topology_instance";
		  final AttributeDefinition[] attributeDefinitions = new AttributeDefinition[] {
				  new AttributeDefinition("nodes", "string", Multiplicity.OPTIONAL, false, null),
				  new AttributeDefinition("topologyReference", "storm_topology", Multiplicity.OPTIONAL, false, null),
		  };

		  addClassTypeDefinition(typeName, ImmutableSet.of("Process"), attributeDefinitions);
		  System.out.println("Created definition for " + typeName);
	}
	
	private void createEventType(){
		  final String typeName = "event";
		  final AttributeDefinition[] attributeDefinitions = new AttributeDefinition[] {
				  new AttributeDefinition("event_key", "string", Multiplicity.OPTIONAL, false, null),
				  new AttributeDefinition("flowFileId", "string", Multiplicity.OPTIONAL, false, null)
		  };

		  addClassTypeDefinition(typeName, ImmutableSet.of("DataSet"), attributeDefinitions);
		  System.out.println("Created definition for " + typeName);
	}
	
	private void addClassTypeDefinition(String typeName, ImmutableSet<String> superTypes, AttributeDefinition[] attributeDefinitions) {
		HierarchicalTypeDefinition<ClassType> definition =
            new HierarchicalTypeDefinition<>(ClassType.class, typeName, null, superTypes, attributeDefinitions);
		classTypeDefinitions.put(typeName, definition);
	}
	
	public ImmutableList<EnumTypeDefinition> getEnumTypeDefinitions() {
		return ImmutableList.copyOf(enumTypeDefinitionMap.values());
	}

	public ImmutableList<StructTypeDefinition> getStructTypeDefinitions() {
		return ImmutableList.copyOf(structTypeDefinitionMap.values());
	}
	
	public ImmutableList<HierarchicalTypeDefinition<TraitType>> getTraitTypeDefinitions() {
		return ImmutableList.of();
	}
	
	private String generateStormEventLineageDataModel(){
		TypesDef typesDef;
		String stormEventLineageDataModelJSON;
		
		try {
			atlasClient.getType("event");
			System.out.println("********************* Nifi Atlas Type: event is already present");
		} catch (AtlasServiceException e) {
			createEventType();
		}
		
		try {
			atlasClient.getType("storm_topology_instance");
			System.out.println("********************* Nifi Atlas Type: storm_topology_instance is already present");
		} catch (AtlasServiceException e) {
			createStormTopologyInstanceType();
		}
		
		typesDef = TypesUtil.getTypesDef(
				getEnumTypeDefinitions(), 	//Enums 
				getStructTypeDefinitions(), //Struct 
				getTraitTypeDefinitions(), 	//Traits 
				ImmutableList.copyOf(classTypeDefinitions.values()));
		
		stormEventLineageDataModelJSON = TypesSerialization.toJson(typesDef);
		System.out.println("Submitting Types Definition: " + stormEventLineageDataModelJSON);
		return stormEventLineageDataModelJSON;
	}
	
	private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
	    InputStream is = new URL(url).openStream();
	    try {
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      String jsonText = readAll(rd);
	      JSONObject json = new JSONObject(jsonText);
	      return json;
	    } finally {
	      is.close();
	    }
	}
	
	private JSONObject readJSONFromUrlAuth(String urlString, String[] basicAuth) throws IOException, JSONException {
		String userPassString = basicAuth[0]+":"+basicAuth[1];
		JSONObject json = null;
		try {
            URL url = new URL (urlString);
            //String encodedUserPassString = Base64.encodeBase64String(userPassString.getBytes());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty  ("Authorization", "Basic " + atlasPasswordEncoding);
            InputStream content = (InputStream)connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(content, Charset.forName("UTF-8")));
  	      	String jsonText = readAll(rd);
  	      	json = new JSONObject(jsonText);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return json;
    }
	
	private String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	}
	
	public static Map<String, String> getFieldValues(Object instance, boolean prependClassName) throws IllegalAccessException {
		Class clazz = instance.getClass();
		Map<String, String> output = new HashMap<>();
		
		for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
			Field[] fields = c.getDeclaredFields();
			for (Field field : fields) {
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}	

				String key;
				if (prependClassName) {
					key = String.format("%s.%s", clazz.getSimpleName(), field.getName());
				} else {
					key = field.getName();
				}

				boolean accessible = field.isAccessible();
				if (!accessible) {
					field.setAccessible(true);
				}
				
				Object fieldVal = field.get(instance);
				if (fieldVal == null) {
					continue;
				} else if (fieldVal.getClass().isPrimitive() || isWrapperType(fieldVal.getClass())) {
					if (toString(fieldVal, false).isEmpty()) continue;
					output.put(key, toString(fieldVal, false));
				} else if (isMapType(fieldVal.getClass())) {
					//TODO: check if it makes more sense to just stick to json
					// like structure instead of a flatten output.
					Map map = (Map) fieldVal;
					for (Object entry : map.entrySet()) {
						Object mapKey = ((Map.Entry) entry).getKey();
						Object mapVal = ((Map.Entry) entry).getValue();

						String keyStr = getString(mapKey, false);
						String valStr = getString(mapVal, false);
						if ((valStr == null) || (valStr.isEmpty())) {
							continue;
						} else {
							output.put(String.format("%s.%s", key, keyStr), valStr);
						}
					}
				} else if (isCollectionType(fieldVal.getClass())) {
					//TODO check if it makes more sense to just stick to
					// json like structure instead of a flatten output.
					Collection collection = (Collection) fieldVal;
					if (collection.size()==0) continue;
					String outStr = "";
					for (Object o : collection) {
						outStr += getString(o, false) + ",";
					}
					if (outStr.length() > 0) {
						outStr = outStr.substring(0, outStr.length() - 1);
					}
					output.put(key, String.format("%s", outStr));
				} else {
					Map<String, String> nestedFieldValues = getFieldValues(fieldVal, false);
					for (Map.Entry<String, String> entry : nestedFieldValues.entrySet()) {
						output.put(String.format("%s.%s", key, entry.getKey()), entry.getValue());
					}
				}
				if (!accessible) {
					field.setAccessible(false);
				}
			}
		}
		return output;
	}
	
	 private static final Set<Class> WRAPPER_TYPES = new HashSet<Class>() {{
		 add(Boolean.class);
		 add(Character.class);
		 add(Byte.class);
		 add(Short.class);
		 add(Integer.class);
		 add(Long.class);
		 add(Float.class);
		 add(Double.class);
		 add(Void.class);
		 add(String.class);
	    }};

	    public static boolean isWrapperType(Class clazz) {
	        return WRAPPER_TYPES.contains(clazz);
	    }

	    public static boolean isCollectionType(Class clazz) {
	        return Collection.class.isAssignableFrom(clazz);
	    }

	    public static boolean isMapType(Class clazz) {
	        return Map.class.isAssignableFrom(clazz);
	    }	

	private static String getString(Object instance,
			boolean wrapWithQuote) throws IllegalAccessException {
		if (instance == null) {
			return null;
		} else if (instance.getClass().isPrimitive() || isWrapperType(instance.getClass())) {
			return toString(instance, wrapWithQuote);
		} else {
			return getString(getFieldValues(instance, false), wrapWithQuote);
		}
	}

	private static String getString(Map<String, String> flattenFields, boolean wrapWithQuote) {
		String outStr = "";
		if (flattenFields != null && !flattenFields.isEmpty()) {
			if (wrapWithQuote) {
				outStr += "\"" + Joiner.on(",").join(flattenFields.entrySet()) + "\",";
			} else {
				outStr += Joiner.on(",").join(flattenFields.entrySet()) + ",";
			}
		}
		
		if (outStr.length() > 0) {
			outStr = outStr.substring(0, outStr.length() - 1);
		}
		return outStr;
	}

	private static String toString(Object instance, boolean wrapWithQuote) {
		if (instance instanceof String)
			if (wrapWithQuote)
				return "\"" + instance + "\"";
			else
				return instance.toString();
		else
			return instance.toString();
	}
}