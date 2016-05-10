package com.jmapper.core.mapper;

import java.util.List;



import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.jmapper.core.exception.ServiceSupportException;


@XmlRootElement(name = "entity")
public class EntityMapper {

	private List<EntityProperty> property;
	
	private String name;
	private String table;
	
	
	public List<EntityProperty> getProperty() {
		return property;
	}
	public void setProperty(List<EntityProperty> property) {
		this.property = property;
	}
	@XmlAttribute
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@XmlAttribute
	public String getTable() throws ServiceSupportException {
		int count=0;
		for(EntityProperty entityProperty : this.property){
			if(entityProperty.isKey()){
				count++;
			}
		}
		if(count==0 || count>1){
			throw new ServiceSupportException("表【"+table+"】中只能设置一个字段为key！");
		}
		return table;
	}
	public void setTable(String table) {
		this.table = table;
	}
	
	
	
	
	
}
