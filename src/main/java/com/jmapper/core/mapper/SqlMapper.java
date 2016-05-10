package com.jmapper.core.mapper;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class SqlMapper {

	private String id;
	private String data;
	
	@XmlAttribute
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
    @XmlValue
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	
	
}
