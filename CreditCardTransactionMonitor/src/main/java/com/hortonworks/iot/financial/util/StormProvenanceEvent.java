package com.hortonworks.iot.financial.util;

public class StormProvenanceEvent {
	private String eventKey;
	private String eventType;	
	private String componentName;
	private String componentType;
	private String sourceDataRepositoryType;
	private String sourceDataRepositoryLocation;
	private String targetDataRepositoryType;
	private String targetDataRepositoryLocation;
	
	public StormProvenanceEvent(){}
	
	public StormProvenanceEvent(String eventKey, String eventType, String componentName, String componentType){
		this.eventKey = eventKey;
		this.eventType = eventType;
		this.componentName = componentName;
		this.componentType = componentType;
	}
	
	public String getEventKey() {
		return eventKey;
	}
	public void setEventKey(String eventKey) {
		this.eventKey = eventKey;
	}
	public String getEventType() {
		return eventType;
	}
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	public String getComponentType() {
		return componentType;
	}
	public void setComponentId(String componentType) {
		this.componentType = componentType;
	}
	public String getComponentName() {
		return componentName;
	}
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	public String getTargetDataRepositoryType() {
		return targetDataRepositoryType;
	}

	public void setTargetDataRepositoryType(String targetDataRepositoryType) {
		this.targetDataRepositoryType = targetDataRepositoryType;
	}

	public String getTargetDataRepositoryLocation() {
		return targetDataRepositoryLocation;
	}

	public void setTargetDataRepositoryLocation(String targetDataRepositoryLocation) {
		this.targetDataRepositoryLocation = targetDataRepositoryLocation;
	}

	public String getSourceDataRepositoryType() {
		return sourceDataRepositoryType;
	}

	public void setSourceDataRepositoryType(String sourceDataRepositoryType) {
		this.sourceDataRepositoryType = sourceDataRepositoryType;
	}

	public String getSourceDataRepositoryLocation() {
		return sourceDataRepositoryLocation;
	}

	public void setSourceDataRepositoryLocation(String sourceDataRepositoryLocation) {
		this.sourceDataRepositoryLocation = sourceDataRepositoryLocation;
	}
	
	
}
