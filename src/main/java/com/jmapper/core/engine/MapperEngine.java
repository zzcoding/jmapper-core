package com.jmapper.core.engine;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.jmapper.core.exception.MappingException;


import com.jmapper.core.mapper.EntityMapper;
import com.jmapper.core.mapper.SqlMapper;
import com.jmapper.core.mapper.SqlTemplateMapper;
import com.jmapper.core.util.JaxbUtil;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
/**
 * 
 * Function: 映射引擎，容器加载时读取映射文件. 
 * Project Name:jmapper-core 
 * File Name:MapperEngine.java 
 * Package Name:com.jmapper.core.engine 
 * Date:2016年4月24日下午10:21:11 
 * Copyright (c) 2016, zinggozhao@163.com All Rights Reserved. 
 * @author 赵广
 */
public class MapperEngine {

	private Logger logger = Logger.getLogger(MapperEngine.class);
	private String entity;
	private String sql;
	private Map<String, EntityMapper> entityCache = new HashMap<String, EntityMapper>();
	private Map<String, String> sqlTemplateCache = new HashMap<String, String>();
	private Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
	private StringTemplateLoader stringLoader = new StringTemplateLoader();  

	@PostConstruct
	public void init() throws Exception {
		//映射实体类
		initEntityMapper();
		//映射sql文件
		initSqlMapper();
	}

	private void initSqlMapper() throws Exception {
		URL base = this.getClass().getResource("/");
		List<File> fileList =new ArrayList<File>();
		findSqlMapperFile(new File(base.getFile()), fileList);
		String xml = null;
		
		for (File file : fileList) {
		
			xml = FileUtils.readFileToString(file,"utf-8");
			logger.info("################开始缓存sql->" + file.getName());
			JaxbUtil resultBinder = new JaxbUtil(SqlTemplateMapper.class, JaxbUtil.CollectionWrapper.class);
			SqlTemplateMapper mapper = resultBinder.fromXml(xml);
			String namespace = mapper.getNamespace();
			logger.info("################mapper namespace->" + namespace);
			for (SqlMapper sql : mapper.getSql()) {
				String cached = sqlTemplateCache.get(namespace + "." + sql.getId());
				if (StringUtils.isBlank(sql.getId())) {
					throw new MappingException("【" + file.getName() + "】中sql mapper id不能为空!");
				}
				if (cached != null) {
					throw new MappingException(namespace + "." + sql.getId() + "在sqlTemplateCache中已存在！");
				} else {
					sqlTemplateCache.put(namespace + "." + sql.getId(), sql.getData());
					stringLoader.putTemplate(namespace + "." + sql.getId(),  sql.getData());
				}
			}
			cfg.setTemplateLoader(stringLoader); 
		}
	}

	private void initEntityMapper() throws Exception {
		URL base = this.getClass().getResource("/");
		List<File> fileList =new ArrayList<File>();
		findEntityMapperFile(new File(base.getFile()), fileList);
		String xml = null;

		for (File file : fileList) {
		
			xml = FileUtils.readFileToString(file);
			logger.info("################开始缓存entity->" + file.getName());
			JaxbUtil resultBinder = new JaxbUtil(EntityMapper.class, JaxbUtil.CollectionWrapper.class);
			EntityMapper mapper = resultBinder.fromXml(xml);
			logger.info("################mapper name->" + mapper.getName());
			if (StringUtils.isBlank(mapper.getName())) {
				throw new MappingException("【" + file.getName() + "】中entity mapper name不能为空!");
			}
			EntityMapper cached = entityCache.get(mapper.getName());
			if (cached != null) {
				throw new MappingException(mapper.getName() + "在entityCache中已存在！");
			} else {
				entityCache.put(mapper.getName(), mapper);
			}
		}

	}
    //读取类路径下所有实体类映射文件
	private void findEntityMapperFile(File file, List<File> fileList) {

		for (File sfile : file.listFiles()) {
			if (sfile.isDirectory()) {
				findEntityMapperFile(sfile, fileList);
			} else {
				if (FilenameUtils.getExtension(sfile.getName()).equals("xml")) {
					Pattern pattern = Pattern.compile(entity);
					Matcher matcher = pattern.matcher(sfile.getName());
					if (matcher.matches()) {
						fileList.add(sfile);
					}

				}
			}
		}

	}
    //读取类路径下所有sql映射文件
	private void findSqlMapperFile(File file, List<File> fileList) {

		for (File sfile : file.listFiles()) {
			if (sfile.isDirectory()) {
				findSqlMapperFile(sfile, fileList);
			} else {
				if (FilenameUtils.getExtension(sfile.getName()).equals("xml")) {
					Pattern pattern = Pattern.compile(sql);
					Matcher matcher = pattern.matcher(sfile.getName());
					if (matcher.matches()) {
						fileList.add(sfile);
					}

				}
			}
		}

	}

	public Map<String, EntityMapper> getEntityCache() {
		return entityCache;
	}

	public void setEntityCache(Map<String, EntityMapper> entityCache) {
		this.entityCache = entityCache;
	}

	public Map<String, String> getSqlTemplateCache() {
		return sqlTemplateCache;
	}

	public void setSqlTemplateCache(Map<String, String> sqlTemplateCache) {
		this.sqlTemplateCache = sqlTemplateCache;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public Configuration getCfg() {
		return cfg;
	}

	public void setCfg(Configuration cfg) {
		this.cfg = cfg;
	}

	public StringTemplateLoader getStringLoader() {
		return stringLoader;
	}

	public void setStringLoader(StringTemplateLoader stringLoader) {
		this.stringLoader = stringLoader;
	}

}
