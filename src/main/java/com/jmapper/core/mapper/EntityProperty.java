package com.jmapper.core.mapper;

import javax.xml.bind.annotation.XmlAttribute;

public class EntityProperty {

	private String name;
	
	private boolean autoincre;
	private String column;
	private boolean key;
	
	@XmlAttribute
	public boolean isAutoincre() {
		return autoincre;
	}
	public void setAutoincre(boolean autoincre) {
		this.autoincre = autoincre;
	}
	@XmlAttribute
	public boolean isKey() {
		return key;
	}
	public void setKey(boolean key) {
		this.key = key;
	}
	@XmlAttribute
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@XmlAttribute
	public String getColumn() {
		return column;
	}
	public void setColumn(String column) {
		this.column = column;
	}
	
	
}
