package org.edu_sharing.service.authentication.sso.config;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.log4j.Logger;



public class MappingGroupBuilderFactory {
	
	static Logger logger = Logger.getLogger(MappingGroupBuilderFactory.class);

	public static MappingGroupBuilder instance(Map<String,String> ssoAttributes, String mappingGroupBuilderClass) {
		if(mappingGroupBuilderClass == null || mappingGroupBuilderClass.trim().equals("")) {
			return null;
		}
		
		try {
			Class clazz = Class.forName(mappingGroupBuilderClass);
			MappingGroupBuilder mgb = (MappingGroupBuilder)clazz.getConstructor(new Class[] { }).newInstance(new Object[] {  });
			mgb.init(ssoAttributes);
			return mgb;
		}catch(ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
		
		return null;
	}
}
