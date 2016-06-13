package com.hortonworks.iot.financial.util;

public class LineageReferenceType {
	private String id;
	private String jsonClass = "org.apache.atlas.typesystem.json.InstanceSerialization$_Id";
    private Integer version = 0;
    private String typeName = "DataSet";
    
    public LineageReferenceType(){}
    
    public LineageReferenceType(String id, String jsonClass, Integer version, String typeName){
    	this.id = id;
    	this.jsonClass = jsonClass;
    	this.version = version;
    	this.typeName = typeName;
    }
    
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getJsonClass() {
		return jsonClass;
	}

	public void setJsonClass(String jsonClass) {
		this.jsonClass = jsonClass;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
}