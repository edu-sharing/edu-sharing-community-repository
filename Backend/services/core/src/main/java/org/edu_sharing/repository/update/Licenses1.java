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
import java.util.List;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

/**
 * renames the value the property assignedlicense of AssignedLicense Objects with  
 * the value "ES - personal use" to an new one
 * 
 * also deletes AssignedLicense Objects that got assignedlicense = "by" or "by-sa"...
 * and writes the cclicense to the property CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY
 * of th IO to a new one
 *
 */
public class Licenses1 implements Update{
	
	PrintWriter out = null;
	
	private static Log logger = LogFactory.getLog(Licenses1.class);
	
	static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
	public static final String description = "renames the value the property assignedlicense of AssignedLicense Objects with the value \"ES - personal use\" to an new one also deletes AssignedLicense Objects that got assignedlicense = \"by\" or \"by-sa\"... and writes the cclicense to the property CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY of th IO to a new one";
	
	public static final String ID = "Licenses1";
	
	public Licenses1(PrintWriter _out){
		out = _out;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public void execute() {
		start(true);
	}
	
	@Override
	public void test() {
		start(false);
	}
	
	private void start(boolean doIt){
		int escounter = 0;
		int cccounter = 0;
		int notchangedcounter = 0;
		try {
			// MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient(authInfo);
			ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
			ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

			SearchService searchService = serviceRegistry.getSearchService();
			NodeService nodeService = serviceRegistry.getNodeService();

			String searchString = "TYPE:\"" + CCConstants.CCM_TYPE_ASSIGNED_LICENSE + "\"";
			ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, searchString);

			logger.info("found " + resultSet.length() + " " + CCConstants.CCM_TYPE_ASSIGNED_LICENSE + "'s");
			for (NodeRef nodeRef : resultSet.getNodeRefs()) {
				String nodeType = nodeService.getType(nodeRef).toString();

				logger.info("");
				logger.info("*****************************************************************");
				logger.info("node: " + nodeRef.getId() + " type:" + nodeType);
				logger.info("*****************************************************************");

				if (!nodeType.equals(CCConstants.CCM_TYPE_ASSIGNED_LICENSE)) {
					logger.error("Update failed! " + nodeType + " is no " + CCConstants.CCM_TYPE_ASSIGNED_LICENSE
							+ " stopping update");

					out.print("Update failed! " + nodeType + " is no " + CCConstants.CCM_TYPE_ASSIGNED_LICENSE
							+ " stopping update");
					return;
				} else {

					List list = (List) nodeService.getProperty(nodeRef, QName
							.createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE));

					String assignedLicense = (String) list.get(0);
					if (assignedLicense == null) {

						logger.info("assignedLicense is null");
						continue;
					}

					logger.info("assigned license:" + assignedLicense);

					if (assignedLicense.equals("ES - personal use")) {
						escounter++;
						logger.info("it's an ES license");
						logger.info("Node: " + nodeRef.getId() + " renaming value of property "
								+ CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE + " ES - personal use to"
								+ CCConstants.COMMON_LICENSE_EDU_P_NR);
						if (doIt)
							nodeService.setProperty(nodeRef, QName
									.createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE),
									CCConstants.COMMON_LICENSE_EDU_P_NR);
						
					} else if (assignedLicense.equals("ES - publish")) {
						escounter++;
						logger.info("it's an ES license");
						logger.info("Node: " + nodeRef.getId() + " renaming value of property "
								+ CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE + " ES - publish to"
								+ CCConstants.COMMON_LICENSE_EDU_NC);
						if (doIt)
							nodeService.setProperty(nodeRef, QName
									.createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE),
									CCConstants.COMMON_LICENSE_EDU_NC);
						
					} else if (assignedLicense.contains("by")) {
						cccounter++;
						
						logger.info("it's an CC license");
						ChildAssociationRef parentAssocRef = nodeService.getPrimaryParent(nodeRef);
						NodeRef parentRef = parentAssocRef.getParentRef();
						String parentNodeType = nodeService.getType(parentRef).toString();
						if (!parentNodeType.equals(CCConstants.CCM_TYPE_IO)) {
							logger.error("Update failed! " + parentNodeType + " is no " + CCConstants.CCM_TYPE_IO);
							out.print("Update failed! " + parentNodeType + " is no " + CCConstants.CCM_TYPE_IO);
							return;
						} else {

							String value = null;
							if (assignedLicense.equals("by")) {
								value = CCConstants.COMMON_LICENSE_CC_BY;
							}
							if (assignedLicense.equals("by-sa")) {
								value = CCConstants.COMMON_LICENSE_CC_BY_SA;
							}
							if (assignedLicense.equals("by-nd")) {
								value = CCConstants.COMMON_LICENSE_CC_BY_ND;
							}
							if (assignedLicense.equals("by-nc")) {
								value = CCConstants.COMMON_LICENSE_CC_BY_NC;
							}
							if (assignedLicense.equals("by-nc-sa")) {
								value = CCConstants.COMMON_LICENSE_CC_BY_NC_SA;
							}
							if (assignedLicense.equals("by-nc-nd")) {
								value = CCConstants.COMMON_LICENSE_CC_BY_NC_ND;
							}
							
							logger.info("IO:" + parentRef.getId() + " setting "
									+ CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY + " val:" + value);
							if (doIt)
								nodeService.setProperty(parentRef, QName
										.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY), value);

							// set Permissions
							String[] permissionsToSet = { PermissionService.READ, CCConstants.PERMISSION_CC_PUBLISH };
							PermissionService permService = serviceRegistry.getPermissionService();
							logger.info("IO:" + parentRef.getId() + " setting permissions for "
									+ PermissionService.ALL_AUTHORITIES);
							for (String permission : permissionsToSet) {
								logger.info("  (setting permission: " + permission + ")");
								if (doIt)
									permService.setPermission(parentRef, PermissionService.ALL_AUTHORITIES, permission, true);
							}

							// delete old
							logger.info("removing AssignedLicense:" + nodeRef.getId() + " from IO:" + parentRef.getId() + "");
							if (doIt) nodeService.removeChild(parentRef, nodeRef);
						}
						
					} else {
						logger.info("no assigned license we should change:" + assignedLicense);
						notchangedcounter++;
					}

				}

			}
			
			if(doIt){
				logger.info("Update End");
				logger.info("ES:"+escounter+" CC:"+cccounter+" dontneedToChange:"+notchangedcounter );
			}else{
				logger.info("Update Test End. No Data changed");
				logger.info("ES:"+escounter+" CC:"+cccounter+" dontneedToChange:"+notchangedcounter );
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			out.print(e.getMessage());
		}
	}
	
}
