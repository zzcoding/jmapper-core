package com.jmapper.core.mapper;

import java.util.List;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "mapper")
public class SqlTemplateMapper {

	private List<SqlMapper> sql;
	
	private String namespace;
	
	@XmlElement
	public List<SqlMapper> getSql() {
		return sql;
	}

	public void setSql(List<SqlMapper> sql) {
		this.sql = sql;
	}

	@XmlAttribute
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	
}
