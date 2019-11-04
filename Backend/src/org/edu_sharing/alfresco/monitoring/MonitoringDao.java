package org.edu_sharing.alfresco.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.tomcat.util.modeler.Registry;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

public class MonitoringDao {

	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	BasicDataSource dataSource = (BasicDataSource) applicationContext.getBean("defaultDataSource");
	
	
	public static final String MAX_ACTIVE = "maxActive";
	public static final String MAX_IDLE = "maxIdle";
	public static final String NUM_ACTIVE = "numActive";
	public static final String NUM_IDLE = "numIdle";
	
	
	public static final String[] mbeanAttributes = new String[]{"maxConnections","maxThreads","maxKeepAliveRequests","minSpareThreads","currentThreadsBusy","currentThreadCount"};
	
	
	
	
	public HashMap<String,String> getDataBasePoolInfo(){
		 HashMap<String,String> map = new HashMap<String,String>();
		 map.put(MAX_ACTIVE, "" + dataSource.getMaxActive());
		 map.put(MAX_IDLE, "" + dataSource.getMaxIdle());
		 map.put(NUM_ACTIVE, "" + dataSource.getNumActive());
		 map.put(NUM_IDLE, "" + dataSource.getNumIdle());
		 return map;
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public String[] getMBeans() throws Exception {
		MBeanServer mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
		String onStr = "*:type=ThreadPool,*";
		ObjectName objectName = new ObjectName(onStr);
		Set<ObjectInstance> set = mBeanServer.queryMBeans(objectName, null);
		Iterator<ObjectInstance> iterator = set.iterator();

		ArrayList<String> names = new ArrayList<String>();
		while (iterator.hasNext()) {
			ObjectInstance oi = iterator.next();
			ObjectName objName = oi.getObjectName();
			String name = objName.getKeyProperty("name");
			names.add(name);
		}

		return names.toArray(new String[0]);
	}

	
	/**
	 * 
	 * @param mbeanName
	 * @param attributes
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, String> getMBeanAttributes(String mbeanName, String[] attributes) throws Exception {

		MBeanServer mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
		String onStr = "*:type=ThreadPool,*";
		ObjectName objectName = new ObjectName(onStr);
		Set<ObjectInstance> set = mBeanServer.queryMBeans(objectName, null);
		Iterator<ObjectInstance> iterator = set.iterator();

		HashMap<String, String> result = new HashMap<String, String>();
		while (iterator.hasNext()) {
			ObjectInstance oi = iterator.next();

			ObjectName objName = oi.getObjectName();
			String name = objName.getKeyProperty("name");

			if (mbeanName.equals(name)) {
				MBeanInfo mbeanInfo = mBeanServer.getMBeanInfo(objName);

				if (attributes == null) {

					for (MBeanAttributeInfo info : mbeanInfo.getAttributes()) {
						try {
							result.put(info.getName(), (String) mBeanServer.getAttribute(objName, info.getName()));
						} catch (javax.management.ReflectionException e) {
						}
					}
				} else {
					for (String attribute : attributes) {
						try {
							result.put(attribute, mBeanServer.getAttribute(objName, attribute).toString());
						} catch (javax.management.ReflectionException e) {

						}
					}
				}
			}

		}

		return result;

	}
	
	
	 public HashMap<Application,Integer> getSessionCount(String host) throws Exception{
		 return new TomcatUtil().getSessionCount(host);
	 }
	
}
