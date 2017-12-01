/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

public class Licenses2 implements Update {
	
	private static Log logger = LogFactory.getLog(Licenses2.class);
	static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
	PrintWriter out = null;
	
	public static final String description = "Alle CC* Lizensen die sich in '{http://www.campuscontent.de/model/1.0}assignedlicense' befinden, nach property {http://www.campuscontent.de/model/1.0}commonlicense_key schreiben";
	
	public static final String ID = "Licenses2";
	
	public Licenses2(PrintWriter _out){
		out = _out;
	}
	
	@Override
	public void execute() {
		start(true);
	}
	
	@Override
	public void test() {
		start(false);
		
	}

	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	private void start(boolean doIt){
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		SearchService searchService = serviceRegistry.getSearchService();
		NodeService nodeService = serviceRegistry.getNodeService();
		
		// go thru all IOs which have direct properties
		String searchString = 	"TYPE:\"{http://www.campuscontent.de/model/1.0}io\" AND @ccm\\:assignedlicense:\"CC*\"";
		ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, searchString);
		logger.info("found "+resultSet.length()+" IO's with direct assignedlicense property");
		int counter = 0;
		for(NodeRef nodeRef:resultSet.getNodeRefs()){
			Serializable propValue = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE));
			String nodeType = nodeService.getType(nodeRef).toString();
			logger.info("");
			logger.info("****************************************************");
			logger.info("NodeId:"+nodeRef.getId() +" nodeType:"+nodeType);
			logger.info("****************************************************");
			if(nodeType.equals(CCConstants.CCM_TYPE_IO)){
				if(propValue instanceof List){
					List propValueList = (List)propValue;
					String value = (String)propValueList.get(0);
					if(value.contains("CC_")){
						counter++;
						logger.info("setting property "+CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY+" to " +value);
						if(doIt) nodeService.setProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY), value);
					}
				}else{
					logger.info("propValue was no list ther is something wrong");
				}
			}else{
				logger.info("NOT AN IO: NodeId:"+nodeRef.getId() +" nodeType:"+nodeType);
			}
		}
		logger.info(counter+ " IO's changed.");
		out.println(counter+ " IO's changed.");
		if(doIt){
			logger.info(" Update ends");
			out.println(" Update ends");
		}else{
			logger.info(" Test ends");
			out.println(counter+ " Test ends");
		}
	}
	
}
