package com.jmapper.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.ReflectionUtils;

import com.jmapper.core.engine.MapperEngine;
import com.jmapper.core.exception.ServiceSupportException;
import com.jmapper.core.mapper.EntityMapper;
import com.jmapper.core.mapper.EntityProperty;
import com.jmapper.core.util.PageModel;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * 
 * Function: 基础支持类. 
 * Project Name:jmapper-core 
 * File Name:ServiceSupport.java 
 * Package Name:com.jmapper.core 
 * Date:2016年4月25日下午1:39:54 
 * Copyright (c) 2016, zinggozhao@163.com All Rights Reserved. 
 * @author 赵广
 */
public class ServiceSupport {

	Logger logger = Logger.getLogger(ServiceSupport.class);
	private final BasicRowProcessor convert = new BasicRowProcessor();
	@Autowired
	protected JdbcTemplate jdbcTemplate;
	@Autowired
	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	@Autowired
	MapperEngine mapperEngine;

	public <T> T save(T entity) {
		Field autoKeyfield = null;
		String className = entity.getClass().getName();
		EntityMapper entityMapper = mapperEngine.getEntityCache().get(className);
		if (entityMapper == null) {
			throw new ServiceSupportException(className + "没有映射！");
		}
		logger.info(entityMapper.getTable());
		final List<Object> params = new ArrayList<Object>();
		StringBuffer buffer = new StringBuffer("insert into ");
		buffer.append(entityMapper.getTable());
		buffer.append("( ");
		StringBuffer feildBuffer = new StringBuffer("");
		StringBuffer valueBuffer = new StringBuffer("");
		for (EntityProperty ep : entityMapper.getProperty()) {
			if (ep.isAutoincre() && ep.isKey()) {
				try {
					autoKeyfield = entity.getClass().getDeclaredField(ep.getName());
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				continue;

			}
			if (feildBuffer.length() == 0) {
				feildBuffer.append(ep.getColumn());
			} else {
				feildBuffer.append(",");
				feildBuffer.append(ep.getColumn());
			}
			if (valueBuffer.length() == 0) {
				valueBuffer.append("?");
				Field field;
				try {
					field = entity.getClass().getDeclaredField(ep.getName());
					field.setAccessible(true);
					params.add(ReflectionUtils.getField(field, entity));
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				
			} else {
				valueBuffer.append(",?");
				Field field;
				try {
					field = entity.getClass().getDeclaredField(ep.getName());
					field.setAccessible(true);
					params.add(ReflectionUtils.getField(field, entity));
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				
			}

		}
		buffer.append(feildBuffer).append(" ) values ( ");
		buffer.append(valueBuffer).append(" ) ");
		logger.info(buffer.toString());
		final String insertSQL = buffer.toString();
		int key = jdbcTemplate.execute(new ConnectionCallback<Integer>() {
			@Override
			public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
				PreparedStatement ps = con.prepareStatement(insertSQL, PreparedStatement.RETURN_GENERATED_KEYS);
				int parameterIndex = 1;
				for (Object param : params) {
					ps.setObject(parameterIndex++, param);
				}
				ps.executeUpdate();
				ResultSet rs = ps.getGeneratedKeys();
				rs.next();
				logger.debug(rs.getInt(1));
				return rs.getInt(1);
			}
		});
		if (autoKeyfield != null) {
			autoKeyfield.setAccessible(true);
			try {
				autoKeyfield.set(entity, key);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return entity;
	}

	public <T> void batchSave(List<T> entityList) {
		if (entityList == null || entityList.size() == 0) {
			throw new ServiceSupportException("entityList为空！");
		}

		T entity = entityList.get(0);
		String className = entity.getClass().getName();
		EntityMapper entityMapper = mapperEngine.getEntityCache().get(className);
		if (entityMapper == null) {
			throw new ServiceSupportException(className + "没有映射！");
		}
		StringBuffer buffer = new StringBuffer("insert into ");
		buffer.append(entityMapper.getTable());
		buffer.append("( ");
		StringBuffer feildBuffer = new StringBuffer("");
		StringBuffer valueBuffer = new StringBuffer("");
		for (EntityProperty ep : entityMapper.getProperty()) {
			if (ep.isAutoincre() && ep.isKey()) {
				continue;
			}
			if (feildBuffer.length() == 0) {
				feildBuffer.append(ep.getColumn());
			} else {
				feildBuffer.append(",");
				feildBuffer.append(ep.getColumn());
			}
			if (valueBuffer.length() == 0) {
				valueBuffer.append("?");

			} else {
				valueBuffer.append(",?");
			}

		}
		buffer.append(feildBuffer).append(" ) values ( ");
		buffer.append(valueBuffer).append(" ) ");
		final String insertSQL = buffer.toString();
		logger.info(insertSQL);
		final List<List<Object>> argList = new ArrayList<List<Object>>();
		for (T t : entityList) {
			List<Object> arg = new ArrayList<Object>();
			for (EntityProperty ep : entityMapper.getProperty()) {
				if (ep.isAutoincre() && ep.isKey()) {
					continue;
				}
				Field field = null;
				try {
					field = entity.getClass().getDeclaredField(ep.getName());
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				field.setAccessible(true);
				arg.add(ReflectionUtils.getField(field, t));
			}
			argList.add(arg);
		}
		jdbcTemplate.execute(new ConnectionCallback<T>() {
			@Override
			public T doInConnection(Connection con) throws SQLException, DataAccessException {
				PreparedStatement ps = con.prepareStatement(insertSQL, PreparedStatement.RETURN_GENERATED_KEYS);
				for (int i = 0; i < argList.size(); i++) {
					int parameterIndex = 1;
					for (Object param : argList.get(i)) {
						ps.setObject(parameterIndex++, param);
					}
					ps.addBatch();
					if (i != 0 && i % 500 == 0) {
						ps.executeBatch();
						ps.clearBatch();
					}
				}
				ps.executeBatch();
				return null;
			}

		});

	}

	public <T> T getEntity(Class<T> requiredType, Object key) throws ServiceSupportException {
		String className = requiredType.getName();
		T entity = null;
		String keyColumnName = null;
		EntityMapper entityMapper = mapperEngine.getEntityCache().get(className);
		StringBuffer sqlBuffer = new StringBuffer("select ");
		StringBuffer tempBuffer = new StringBuffer("");
		if (entityMapper == null) {
			throw new ServiceSupportException(className + "没有映射！");
		}
		try {
			for (EntityProperty ep : entityMapper.getProperty()) {
				if (ep.isKey()) {
					keyColumnName = ep.getColumn();
				}
				if (tempBuffer.length() == 0) {
					tempBuffer.append(ep.getColumn()).append(" as ").append(ep.getName());
					;
				} else {
					tempBuffer.append(",").append(ep.getColumn()).append(" as ").append(ep.getName());
				}
			}
			sqlBuffer.append(tempBuffer).append(" from ").append(entityMapper.getTable()).append(" where ").append(keyColumnName).append(" = ?");
			entity = queryForEntitySimple(sqlBuffer.toString(), requiredType, new Object[] { key });
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entity;
	}

	public <T> List<T> getAllEntity(Class<T> requiredType) {
		String className = requiredType.getName();
		List<T> entityList = null;
		EntityMapper entityMapper = mapperEngine.getEntityCache().get(className);
		StringBuffer sqlBuffer = new StringBuffer("select ");
		StringBuffer tempBuffer = new StringBuffer("");
		if (entityMapper == null) {
			throw new ServiceSupportException(className + "没有映射！");
		}
		try {
			for (EntityProperty ep : entityMapper.getProperty()) {
				if (tempBuffer.length() == 0) {
					tempBuffer.append(ep.getColumn()).append(" as ").append(ep.getName());
					;
				} else {
					tempBuffer.append(",").append(ep.getColumn()).append(" as ").append(ep.getName());
				}
			}
			sqlBuffer.append(tempBuffer).append(" from ").append(entityMapper.getTable());
			entityList = queryForEntityListSimple(sqlBuffer.toString(), requiredType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entityList;
	}

	public Map<String, Object> queryForMapSimple(String sql, Object... args) {
		Map<String, Object> resultMap = null;
		try {
			resultMap = jdbcTemplate.queryForMap(sql, args);
		} catch (IncorrectResultSizeDataAccessException e) {
			logger.debug("queryForMapSimple未查询到唯一结果集！");
		} catch (IncorrectResultSetColumnCountException e) {
			logger.debug("queryForMapSimple只能够以一列为结果集！");
		}

		return resultMap;
	}

	public Map<String, Object> queryForMapSimpleByMapper(String mapper, Object... args) {
		Map<String, Object> resultMap = null;
		try {

			Configuration cfg = mapperEngine.getCfg();
			Template template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(null, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			resultMap = jdbcTemplate.queryForMap(resultSql, args);
		} catch (IncorrectResultSizeDataAccessException e) {
			logger.debug("queryForMapSimpleByMapper未查询到唯一结果集！");
		} catch (IncorrectResultSetColumnCountException e) {
			logger.debug("queryForMapSimpleByMapper只能够以一列为结果集！");
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultMap;
	}

	public Map<String, Object> queryForMapNamedParameterByMapper(String mapper, Map<String, Object> parameterMap) {
		Map<String, Object> resultMap = null;
		try {
			Configuration cfg = mapperEngine.getCfg();
			Template template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(parameterMap, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			resultMap = namedParameterJdbcTemplate.queryForMap(resultSql, parameterMap);
		} catch (IncorrectResultSizeDataAccessException e) {
			logger.debug("queryForMapNamedParameterByMapper未查询到唯一结果集！");
		} catch (IncorrectResultSetColumnCountException e) {
			logger.debug("queryForMapNamedParameterByMapper只能够以一列为结果集！");
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return resultMap;
	}

	public Map<String, Object> queryForMapNamedParameter(String mapper, Map<String, Object> parameterMap) {
		Map<String, Object> resultMap = null;
		try {

			resultMap = namedParameterJdbcTemplate.queryForMap(mapper, parameterMap);
		} catch (IncorrectResultSizeDataAccessException e) {
			logger.debug("queryForMapNamedParameter未查询到唯一结果集！");
		} catch (IncorrectResultSetColumnCountException e) {
			logger.debug("queryForMapNamedParameter只能够以一列为结果集！");
		}

		return resultMap;
	}

	public <T> List<T> queryForListSimpleByMapper(String mapper, Class<T> requiredType, Object... args) {
		Configuration cfg = mapperEngine.getCfg();
		Template template;
		List<T> resultList = null;
		try {
			template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(null, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			resultList = jdbcTemplate.queryForList(resultSql, requiredType, args);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultList;
	}

	// 只能查询一个字段
	public <T> List<T> queryForListNamedParameterByMapper(String mapper, Class<T> requiredType, Map<String, ?> parameterMap) {
		Configuration cfg = mapperEngine.getCfg();
		Template template;
		List<T> resultList = null;
		try {
			template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(parameterMap, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			resultList = namedParameterJdbcTemplate.queryForList(resultSql, parameterMap, requiredType);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultList;
	}

	public List<Map<String, Object>> queryForListMapSimple(String sql, Object... args) {
		return jdbcTemplate.queryForList(sql, args);
	}

	public List<Map<String, Object>> queryForListMapNamedParameter(String sql, Map<String, ?> parameterMap) {
		return namedParameterJdbcTemplate.queryForList(sql, parameterMap);
	}

	public List<Map<String, Object>> queryForListMapSimpleByMapper(String mapper, Object... args) {
		Configuration cfg = mapperEngine.getCfg();
		List<Map<String, Object>> resultList = null;
		Template template;
		try {
			template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(null, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			resultList = jdbcTemplate.queryForList(resultSql, args);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return resultList;
	}

	public List<Map<String, Object>> queryForListMapNamedParameterByMapper(String mapper, Map<String, ?> parameterMap) {
		Configuration cfg = mapperEngine.getCfg();
		List<Map<String, Object>> resultList = null;
		Template template;
		try {
			template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(parameterMap, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			resultList = namedParameterJdbcTemplate.queryForList(resultSql, parameterMap);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return resultList;
	}

	public int executeUdateSimple(String mapper, Object... args) {
		Configuration cfg = mapperEngine.getCfg();
		int effCount = 0;
		Template template;
		try {
			template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(null, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			effCount = jdbcTemplate.update(resultSql, args);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return effCount;
	}

	public int executeUpdateNamedParameterByMapper(String mapper, Map<String, ?> parameterMap) {
		Configuration cfg = mapperEngine.getCfg();
		int effCount = 0;
		Template template;
		try {
			template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(parameterMap, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			effCount = namedParameterJdbcTemplate.update(resultSql, parameterMap);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return effCount;
	}

	public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
		T t = null;
		try {
			t = jdbcTemplate.queryForObject(sql, requiredType, args);
		} catch (IncorrectResultSizeDataAccessException e) {
			logger.debug("queryForObject未查询到唯一结果集！");
		} catch (IncorrectResultSetColumnCountException e) {
			logger.debug("queryForObject只能够以一列为结果集");
		}
		return t;
	}

	public <T> T queryForObjectByMapper(String mapper, Class<T> requiredType, Object... args) {
		T t = null;
		Configuration cfg = mapperEngine.getCfg();
		Template template;
		try {
			template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(null, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			t = jdbcTemplate.queryForObject(resultSql, requiredType, args);
		} catch (IncorrectResultSizeDataAccessException e) {
			logger.debug("queryForObjectByMapper未查询到唯一结果集！");
		} catch (IncorrectResultSetColumnCountException e) {
			logger.debug("queryForObjectByMapper只能够以一列为结果集");
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return t;
	}

	public int queryForIntByMapper(String mapper, Object... args) {
		int count = 0;
		Configuration cfg = mapperEngine.getCfg();
		Template template;
		try {
			template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(null, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			count = jdbcTemplate.queryForInt(resultSql, args);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return count;
	}

	public int queryForIntNamedParameterByMapper(String mapper, Map<String, ?> parameterMap) {
		int count = 0;
		Configuration cfg = mapperEngine.getCfg();
		Template template;
		try {
			template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(parameterMap, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);
			count = namedParameterJdbcTemplate.queryForInt(resultSql, parameterMap);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return count;
	}

	public String getTemplateSql(String mapper) {
		String resultSql = null;
		try {
			Configuration cfg = mapperEngine.getCfg();
			Template template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(null, new OutputStreamWriter(baos));
			resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);

		} catch (Exception e) {
			logger.info(e);
		} 
		return resultSql;

	}

	public int queryForIntNamedParameter(String countSql, Map<String,Object> parameterMap){
		int totalCount =0;
		try{
			totalCount = namedParameterJdbcTemplate.queryForInt(countSql, parameterMap);
		} catch (IncorrectResultSizeDataAccessException e) {
			logger.debug("queryForIntNamedParameter未查询到唯一结果集！");
		}
		return totalCount;
		
	}
	
	public String getTemplateSql(String mapper, Map<String, Object> paramaterMap) {
		String resultSql = null;
		try {
			Configuration cfg = mapperEngine.getCfg();
			Template template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(paramaterMap, new OutputStreamWriter(baos));
			resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			logger.info(resultSql);

		} catch (Exception e) {
			logger.info(e);
		}
		return resultSql;

	}

	public List<Map<String, Object>> queryForPageListMapNamedParameterByMapper(String mapper, PageModel pageModel, Map<String, Object> parameterMap) {
		List<Map<String, Object>> resultList = null;
		try {
			Configuration cfg = mapperEngine.getCfg();
			Template template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(parameterMap, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			String countSql = getCountSqlFromOrgSql(mapper, resultSql, parameterMap);
			int totalCount = queryForIntNamedParameter(countSql, parameterMap);
			pageModel.setRecordCount(totalCount);
			if (totalCount == 0) {
				return new ArrayList<Map<String, Object>>();
			}
			resultSql = resultSql + " limit :startrow,:pageSize";
			logger.debug(resultSql);
			parameterMap.put("startrow", pageModel.getStartRow());
			parameterMap.put("pageSize", pageModel.getPageSize());
			resultList = namedParameterJdbcTemplate.queryForList(resultSql, parameterMap);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultList;
	}

	public List<Map<String, Object>> queryForPageListMapSimpleByMapper(String mapper, PageModel pageModel, Object... args) {
		List<Map<String, Object>> resultList = null;
		try {
			Configuration cfg = mapperEngine.getCfg();
			Template template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(null, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			String countSql = getCountSqlFromOrgSql(mapper, resultSql);
			int totalCount = jdbcTemplate.queryForInt(countSql, args);
			pageModel.setRecordCount(totalCount);
			if (totalCount == 0) {
				return new ArrayList<Map<String, Object>>();
			}
			resultSql = resultSql + " limit ?,?";
			logger.debug(resultSql);
			ArrayList<Object> params = new ArrayList<Object>();
			for (Object arg : args) {
				params.add(arg);
			}
			params.add(pageModel.getStartRow());
			params.add(pageModel.getPageSize());
			resultList = jdbcTemplate.queryForList(resultSql, params.toArray());
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultList;
	}

	private String getCountSqlFromOrgSql(String mapper, String resultSql) {

		String templateSql = getTemplateSql(mapper + ".count");
		String countSql = "";
		if (templateSql == null) {
			int formIndex = resultSql.indexOf("from") == -1 ? resultSql.indexOf("FROM") : resultSql.indexOf("from");
			countSql = resultSql.substring(formIndex);
			int orderIndex = countSql.indexOf("order") == -1 ? countSql.indexOf("ORDER") : countSql.indexOf("order");
			String removeOrder = orderIndex == -1 ? countSql : countSql.substring(0, orderIndex);
			countSql = "select count(1) " + removeOrder;
		} else {
			countSql = templateSql;
		}
		return countSql;
	}

	private String getCountSqlFromOrgSql(String mapper, String resultSql, Map<String, Object> parameterMap) {

		String templateSql = getTemplateSql(mapper + ".count", parameterMap);
		String countSql = "";
		if (templateSql == null) {
			int formIndex = resultSql.indexOf("from") == -1 ? resultSql.indexOf("FROM") : resultSql.indexOf("from");
			countSql = resultSql.substring(formIndex);
			int orderIndex = countSql.indexOf("order") == -1 ? countSql.indexOf("ORDER") : countSql.indexOf("order");
			String removeOrder = orderIndex == -1 ? countSql : countSql.substring(0, orderIndex);
			countSql = "select count(1) " + removeOrder;
		} else {
			countSql = templateSql;
		}
		return countSql;
	}

	public <T> List<T> queryForPageEntityListSimpleByMapper(String mapper, Class<T> requiredType, PageModel pageModel, Object... args) {
		List<T> resultList = null;
		try {
			Configuration cfg = mapperEngine.getCfg();
			Template template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(null, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			String countSql = getCountSqlFromOrgSql(mapper, resultSql);
			int totalCount = jdbcTemplate.queryForInt(countSql, args);
			pageModel.setRecordCount(totalCount);
			if (totalCount == 0) {
				return (List<T>) new ArrayList<T>();
			}
			resultSql = resultSql + " limit ?,?";
			logger.debug(resultSql);
			ArrayList<Object> params = new ArrayList<Object>();
			for (Object arg : args) {
				params.add(arg);
			}
			params.add(pageModel.getStartRow());
			params.add(pageModel.getPageSize());
			resultList = queryForEntityListSimple(resultSql, requiredType, params.toArray());
			;
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultList;
	}

	public <T> List<T> queryForPageEntityListNamedParameterByMapper(String mapper, Class<T> requiredType, PageModel pageModel, Map<String, Object> parameterMap) {
		List<T> resultList = null;
		try {
			Configuration cfg = mapperEngine.getCfg();
			Template template = cfg.getTemplate(mapper, "utf-8");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.process(parameterMap, new OutputStreamWriter(baos));
			String resultSql = baos.toString();
			resultSql = removeBlank(resultSql);
			String countSql = getCountSqlFromOrgSql(mapper, resultSql, parameterMap);
			int totalCount = namedParameterJdbcTemplate.queryForInt(countSql, parameterMap);
			pageModel.setRecordCount(totalCount);
			if (totalCount == 0) {
				return (List<T>) new ArrayList<T>();
			}
			resultSql = resultSql + " limit :startrow,:pageSize";
			logger.debug(resultSql);
			parameterMap.put("startrow", pageModel.getStartRow());
			parameterMap.put("pageSize", pageModel.getPageSize());
			resultList = queryForEntityListNamedParameter(resultSql, requiredType, parameterMap);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultList;
	}

	public <T> List<T> queryForEntityListNamedParameter(String sql, final Class<T> requiredType, Map<String, Object> parameterMap) {
		return namedParameterJdbcTemplate.query(sql, parameterMap, new RowMapper<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T mapRow(ResultSet rs, int rowNum) throws SQLException {

				return (T) ServiceSupport.this.convert.toBean(rs, requiredType);
			}
		});
	}

	public <T> List<T> queryForEntityListSimple(String sql, final Class<T> requiredType, Object... args) {
		return jdbcTemplate.query(sql, new RowMapper<T>() {

			@Override
			@SuppressWarnings("unchecked")
			public T mapRow(ResultSet rs, int rowNum) throws SQLException {

				return (T) ServiceSupport.this.convert.toBean(rs, requiredType);
			}
		}, args);
	}

	public <T> T queryForEntityNamedParameter(String sql, final Class<T> requiredType, Map<String, Object> parameterMap) {
		T obj = null;
		try {
			namedParameterJdbcTemplate.queryForObject(sql, parameterMap, new RowMapper<T>() {

				@SuppressWarnings("unchecked")
				@Override
				public T mapRow(ResultSet rs, int rowNum) throws SQLException {

					return (T) ServiceSupport.this.convert.toBean(rs, requiredType);
				}
			});

		} catch (EmptyResultDataAccessException e) {
			logger.debug("queryForEntity未查询到");
		}
		return obj;
	}

	public <T> T queryForEntitySimple(String sql, final Class<T> requiredType, Object... args) {
		T obj = null;
		try {
			obj = jdbcTemplate.queryForObject(sql, args, new RowMapper<T>() {
				@SuppressWarnings("unchecked")
				public T mapRow(ResultSet rs, int rowNum) throws SQLException {
					return (T) ServiceSupport.this.convert.toBean(rs, requiredType);
				}
			});

		} catch (EmptyResultDataAccessException e) {
			logger.debug("queryForEntity未查询到");
		}
		return obj;
	}
	
	public  <T> void generUpdateByRequirdEntity(T newEntity,T oldEntity,String condition,Map<String,Object> parameterMap){
		String className = newEntity.getClass().getName();
		EntityMapper entityMapper = mapperEngine.getEntityCache().get(className);
		if (entityMapper == null) {
			throw new ServiceSupportException(className + "没有映射！");
		}
		logger.info(entityMapper.getTable());
		int affectFieldCount=0;
		StringBuffer sql =new StringBuffer("update ");
		sql.append(entityMapper.getTable());
		sql.append(" set ");
		for (EntityProperty ep : entityMapper.getProperty()) {
				try {
					Field field = newEntity.getClass().getDeclaredField(ep.getName());
					field.setAccessible(true);
					Object newEntityValue = ReflectionUtils.getField(field, newEntity);
					Object oldEntityValue = ReflectionUtils.getField(field, oldEntity);
					if(newEntityValue==null)
						continue;
					if(!newEntityValue.equals(oldEntityValue)){
						if(affectFieldCount==0){
							sql.append(ep.getColumn());
							sql.append(" =");
							sql.append(":"+ep.getName());
							parameterMap.put(ep.getName(), newEntityValue);
						}else{
							sql.append(" , ");
							sql.append(ep.getColumn());
							sql.append(" =");
							sql.append(":"+ep.getName());
							parameterMap.put(ep.getName(), newEntityValue);
						}
						affectFieldCount++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
		}
		sql.append(" "+condition);
		logger.info(sql.toString());
		if(affectFieldCount==0){
			logger.info("没有值变化的字段！");
			return;
		}
		namedParameterJdbcTemplate.update(sql.toString(), parameterMap);
	}
	
	public String removeBlank(String sql) {
		String resultSql = "";
		Pattern p = Pattern.compile("\r|\n|\t");
		Matcher m = p.matcher(sql);
		resultSql = m.replaceAll(" ");
		resultSql = resultSql.trim();
		resultSql = resultSql.replaceAll("\\s{1,}", " ");
		return resultSql;
	}

}
