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
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;

import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasException;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.json.InstanceSerialization;
import org.apache.atlas.typesystem.TypesDef;
import org.apache.atlas.typesystem.json.TypesSerialization;
import org.apache.atlas.typesystem.types.AttributeDefinition;
import org.apache.atlas.typesystem.types.ClassType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.atlas.typesystem.types.EnumType;
import org.apache.atlas.typesystem.types.EnumTypeDefinition;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.apache.atlas.typesystem.types.Multiplicity;
import org.apache.atlas.typesystem.types.StructTypeDefinition;
import org.apache.atlas.typesystem.types.TraitType;
import org.apache.atlas.typesystem.types.utils.TypesUtil;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.hortonworks.iot.financial.util.LineageReferenceType;
import com.hortonworks.iot.financial.util.StormProvenanceEvent;

import backtype.storm.generated.SpoutSpec;
import backtype.storm.generated.TopologyInfo;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.utils.Utils;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;

public class AtlasLineageReporter extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	private Map<String,SpoutSpec> spouts; 
	private Map topologyConf;
	private Referenceable topology;
	public static final String ANONYMOUS_OWNER = "anonymous";
	public static final String HBASE_NAMESPACE_DEFAULT = "default";
	public static final String CLUSTER_NAME_KEY = "atlas.cluster.name";
	public static final String DEFAULT_CLUSTER_NAME = "primary";
	public static final String CLUSTER_NAME_ATTRIBUTE = "clusterName";
	public static final String SYSTEM_PROPERTY_APP_PORT = "atlas.app.port";
	public static final String DEFAULT_APP_PORT_STR = "21000";
	public static final String ATLAS_REST_ADDRESS_KEY = "atlas.rest.address";
	public static final String DEFAULT_ATLAS_REST_ADDRESS = "http://localhost:21000";
	private String atlasUrl = DEFAULT_ATLAS_REST_ADDRESS;
	private AtlasClient atlasClient;
	private String atlasVersion;
	private final Map<String, HierarchicalTypeDefinition<ClassType>> classTypeDefinitions = new HashMap<>();
	
	@SuppressWarnings("unused")
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
			topology = createTopologyInstance(topologyConf, incomingEvent, outgoingEvent, lineage);
			System.out.println("********************* Processing Topology: " + topology.getValuesMap().get("id").toString());
			System.out.println("********************* Processing Topology: " + topology.getValuesMap().get("inputs").toString());
			System.out.println("********************* Processing Topology: " + topology.getValuesMap().get("outputs").toString());
			register(atlasClient, topology);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Referenceable getEventReference(StormProvenanceEvent event) throws Exception {
		final String typeName = "event";
		final String id = event.getEventKey();
		 
		String dslQuery = String.format("%s where %s = '%s'", typeName, "name", id);
		System.out.println("********************* Atlas Version is: " + atlasVersion);
		Referenceable eventReferenceable = null;
		if(atlasVersion.equalsIgnoreCase("0.5"))
			eventReferenceable = getEntityReferenceFromDSL5(atlasClient, typeName, dslQuery);
		else if(atlasVersion.equalsIgnoreCase("0.6"))
			eventReferenceable = getEntityReferenceFromDSL6(atlasClient, typeName, dslQuery);
		else
			eventReferenceable = null;
		
		return eventReferenceable;
	}
	
	private Referenceable getBoltReference() throws Exception {
		final String typeName = "storm_bolt";
		//final String id = event.getEventKey();
		 
		String dslQuery = String.format("%s where %s = '%s'", typeName, "name", "count");
		if(atlasVersion.equalsIgnoreCase("0.5"))
			return getEntityReferenceFromDSL5(atlasClient, typeName, dslQuery);
		else if(atlasVersion.equalsIgnoreCase("0.6"))
			return getEntityReferenceFromDSL6(atlasClient, typeName, dslQuery);
		else
			return null;
	}
	
	public Referenceable register(final AtlasClient atlasClient, final Referenceable referenceable) throws Exception {
        if (referenceable == null) {
            return null;
        }

        final String typeName = referenceable.getTypeName();
        System.out.println("creating instance of type " + typeName);

        final String entityJSON = InstanceSerialization.toJson(referenceable, true);
        System.out.println("Submitting new entity " + referenceable.getTypeName() + ":" + entityJSON);

        //final JSONArray guid = atlasClient.createEntity(entityJSON); client vesion 0.6
        final JSONObject guid = atlasClient.createEntity(entityJSON);
        
        //System.out.println("created instance for type " + typeName + ", guid: " + guid); client version 0.6
        System.out.println("created instance for type " + typeName + ", guid: " + guid.getString("GUID"));
        
        //return new Referenceable(guid.getString(0), referenceable.getTypeName(), null); client version 0.6
        return new Referenceable(guid.getString("GUID"), referenceable.getTypeName(), null);
    }
        
    private Referenceable createTopologyInstance(Map stormConf, Referenceable inputEvent, Referenceable outputEvent, List<String> lineage) throws Exception {
        String jsonClass = "org.apache.atlas.typesystem.json.InstanceSerialization$_Id";
        Integer version = 0;
        String typeName = "DataSet";
        LineageReferenceType[] inputs = {new LineageReferenceType(inputEvent.getId()._getId().replace("[", "").replace("]", "").replace("\"", "").replace("\\", ""), jsonClass, version, typeName)};
        LineageReferenceType[] outputs = {new LineageReferenceType(outputEvent.getId()._getId().replace("[", "").replace("]", "").replace("\"", "").replace("\\", ""), jsonClass, version, typeName)};
        //typeName = "storm_node";
        //LineageReferenceType[] nodes = {new LineageReferenceType(getBoltReference().getId()._getId().replace("[", "").replace("]", "").replace("\"", "").replace("\\", ""), jsonClass, version, typeName)};
    	String topologyName = "CreditCardTransactionMonitor" + inputs[0].getId();
    	Referenceable topologyReferenceable = new Referenceable("storm_topology_reference");
    	topologyReferenceable.set("id", topologyName);
    	topologyReferenceable.set("name", topologyName);
    	//topologyReferenceable.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, topologyInfo.get_name());
    	String owner = "";//topologyInfo.get_owner();
    	if (StringUtils.isEmpty(owner)) {
    		owner = ANONYMOUS_OWNER;
    	}
    	topologyReferenceable.set("owner", owner);
    	topologyReferenceable.set("startTime", System.currentTimeMillis());
    	topologyReferenceable.set(CLUSTER_NAME_ATTRIBUTE, getClusterName(stormConf));
    	topologyReferenceable.set("inputs", inputs);
    	topologyReferenceable.set("outputs", outputs);
    	topologyReferenceable.set("nodes", Arrays.toString(lineage.toArray()));
    	//topologyReferenceable.set("nodes", nodes);

    	return topologyReferenceable;
    }        

    private String getClusterName(Map stormConf) {
        String clusterName = DEFAULT_CLUSTER_NAME;
        if (stormConf.containsKey(CLUSTER_NAME_KEY)) {
            clusterName = (String)stormConf.get(CLUSTER_NAME_KEY);
        }
        return clusterName;
    }
    
	public void prepare(Map map, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.spouts = context.getRawTopology().get_spouts();	
		this.atlasClient = new AtlasClient(DEFAULT_ATLAS_REST_ADDRESS);
		this.topologyConf = map;
		this.atlasVersion = getAtlasVersion(atlasUrl + "/api/atlas/admin/version");
		createAtlasDataModel();
	}
	
	private void createStormTopologyReferenceClass() throws AtlasException {
		String type = "storm_topology_reference";
		AttributeDefinition[] attributeDefinitions = new AttributeDefinition[]{
                new AttributeDefinition("nodes", DataTypes.STRING_TYPE.getName(), Multiplicity.OPTIONAL, false, null)};
         
        HierarchicalTypeDefinition<ClassType> definition =
                new HierarchicalTypeDefinition<>(ClassType.class, type,
                	ImmutableList.of(AtlasClient.PROCESS_SUPER_TYPE), attributeDefinitions);
        
        classTypeDefinitions.put(type, definition);
        System.out.println("Created definition for " + type);
    }
	
	private void createEventClass() throws AtlasException {
        String type = "event";
		AttributeDefinition[] attributeDefinitions = new AttributeDefinition[]{
                new AttributeDefinition("event_key", DataTypes.STRING_TYPE.getName(), Multiplicity.OPTIONAL, false, null)};
         
        HierarchicalTypeDefinition<ClassType> definition =
                new HierarchicalTypeDefinition<>(ClassType.class, type,
                	ImmutableList.of(AtlasClient.DATA_SET_SUPER_TYPE), attributeDefinitions);
        
        classTypeDefinitions.put(type, definition);
        System.out.println("Created definition for " + type);
    }

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("ProvenanceEvent"));
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
	
	//Use this version of method when Flow File UUID has been assigned
    private Referenceable createEvent(StormProvenanceEvent event) {
        final String flowFileUuid = event.getEventKey();
        
        // TODO populate processor properties and determine real parent group, assuming root group for now
        final Referenceable processor = new Referenceable("event");
        processor.set("name", flowFileUuid);
        processor.set("event_key", "accountNumber");
        processor.set("description", flowFileUuid);
        return processor;
    }
    
    //Use this version of method when incoming event is ingested and Flow File UUID has not yet been assigned
    private Referenceable createEvent(StormProvenanceEvent event, final String uuid) {
        final Referenceable processor = new Referenceable("event");
        processor.set("name", uuid);
        processor.set("event_key", "accountNumber");
        processor.set("description", uuid);
        return processor;
    }
	
	public static Referenceable getEntityReferenceFromDSL5(final AtlasClient atlasClient, final String typeName, final String dslQuery)
            throws Exception {
		System.out.println("****************************** Query String: " + dslQuery);
        final JSONArray results = atlasClient.searchByDSL(dslQuery);
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
	
	private Referenceable getEntityReferenceFromDSL6(final AtlasClient atlasClient, final String typeName, final String dslQuery)
	           throws Exception {
		   System.out.println("****************************** Query String: " + dslQuery);
		   
	       //JSONArray results = atlasClient.searchByDSL(dslQuery);
	       JSONArray results = searchDSL(atlasUrl + "/api/atlas/discovery/search/dsl?query=", dslQuery);
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
	
	private String getAtlasVersion(String uri){
		System.out.println("************************ Getting Atlas Version from: " + uri);
		JSONObject json = null;
		String versionValue = null;
        try{
        	json = readJsonFromUrl(uri);
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
	
	private String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	}
	
	private void createAtlasDataModel(){
		String stormTopologyType = "{\"enumTypes\": [],"
								+ "\"structTypes\": [],"
								+ "\"traitTypes\": [],"
								+ "\"classTypes\": ["
								+ "{\"superTypes\":[\"Process\"],"
								+ "\"hierarchicalMetaTypeName\":\"org.apache.atlas.typesystem.types.ClassType\","
								+ "\"typeName\":\"storm_topology_reference\","
								+ "\"attributeDefinitions\": ["
								+ "{\"name\": \"nodes\","
								+ "\"dataTypeName\": \"string\","
								+ "\"multiplicity\": \"optional\","
								+ "\"isComposite\": false,"
								+ "\"isUnique\": false,"
								+ "\"isIndexable\": true,"
								+ "\"reverseAttributeName\": null}]}]}";
		String nifiFlowType = "{\"enumTypes\": [],"
							   + "\"structTypes\": [],"
							   + "\"traitTypes\": [],"
							   + "\"classTypes\": ["
							   + "{\"superTypes\":[\"Process\"],"
							   + "\"hierarchicalMetaTypeName\":\"org.apache.atlas.typesystem.types.ClassType\","
							   + "\"typeName\": \"nifi_flow\","
							   + "\"attributeDefinitions\": ["
							   + "{\"name\": \"nodes\","
							   + "\"dataTypeName\": \"string\","
							   + "\"multiplicity\": \"optional\","
							   + "\"isComposite\": false,"
							   + "\"isUnique\": false,"
							   + "\"isIndexable\": true,"
							   + "\"reverseAttributeName\": null},"
							   + "{\"name\": \"flow_id\","
							   + "\"dataTypeName\": \"string\","
							   + "\"multiplicity\": \"optional\","
							   + "\"isComposite\": false,"
							   + "\"isUnique\": false,"
							   + "\"isIndexable\": true,"
							   + "\"reverseAttributeName\": null}]}]}";
		String eventType = "{\"enumTypes\": [],"
				   			+ "\"structTypes\": [],"
				   			+ "\"traitTypes\": [],"
				   			+ "\"classTypes\": ["
				   			+ "{\"superTypes\":[\"DataSet\"],"
				   			+ "\"hierarchicalMetaTypeName\":\"org.apache.atlas.typesystem.types.ClassType\","
				   			+ "\"typeName\":\"event\","
				   			+ "\"attributeDefinitions\":[{\"name\":\"event_key\","
				   			+ "\"dataTypeName\":\"string\","
				   			+ "\"multiplicity\":\"optional\","
				   			+ "\"isComposite\": false,"
				   			+ "\"isUnique\": false,"
				   			+ "\"isIndexable\": true,"
				   			+ "\"reverseAttributeName\": null}]}]}";
		
		try {
			if(atlasClient.getType("storm_topology_reference") == null){
				atlasClient.createType(stormTopologyType);
			}else{
				System.out.println("Atlas Type: storm_topology_reference already exists");
			}
			if(atlasClient.getType("nifi_flow") == null){
				atlasClient.createType(nifiFlowType);
			}else{
				System.out.println("Atlas Type: nifi_flow already exists");
			}
			if(atlasClient.getType("event") == null){
				atlasClient.createType(eventType);
			}else{
				System.out.println("Atlas Type: event already exists");
			}
		} catch (AtlasServiceException e) {
			e.printStackTrace();
		}
	}
}