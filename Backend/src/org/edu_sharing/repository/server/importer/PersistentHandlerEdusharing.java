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
package org.edu_sharing.repository.server.importer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.BehaviourFilterImpl;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.Constants;
import org.springframework.context.ApplicationContext;

public class PersistentHandlerEdusharing implements PersistentHandlerInterface {

	Log logger = LogFactory.getLog(PersistentHandlerEdusharing.class);

	MCAlfrescoBaseClient mcAlfrescoBaseClient = null;

	// for checking if node already excists
	HashMap<String, HashMap<String, Object>> allNodesInImportfolder = null;

	//
	HashMap<String, String> replIdTimestampMap = null;

	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss");
	
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	public PersistentHandlerEdusharing() throws Throwable {
		mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
	}

	public void removeAllImportedObjects() throws Throwable {

		HashMap<String, Object> importFolderProps = getImportFolderProps();
		if (importFolderProps != null) {
			String importFolderNodeId = (String) importFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
			HashMap children = (HashMap) mcAlfrescoBaseClient.getChildren(importFolderNodeId);
			if (children != null) {
				for (Object setKey : children.keySet()) {
					String name = (String) ((HashMap) children.get(setKey)).get(CCConstants.CM_NAME);
					logger.info("removing set:" + name);
					HashMap setCursorFolders = (HashMap) mcAlfrescoBaseClient.getChildren((String) setKey);
					for (Object setCursorfolderId : setCursorFolders.keySet()) {
						HashMap setCursorFolderProps = (HashMap) setCursorFolders.get(setCursorfolderId);
						String setCursorFolderName = (String) setCursorFolderProps.get(CCConstants.CM_NAME);
						logger.info("removing cursor folder:" + setCursorFolderName + " (set:" + name + ")");
						mcAlfrescoBaseClient.removeNode((String) setCursorfolderId, (String) setKey,false);
					}
					// mcAlfrescoBaseClient.removeNode( (String)setKey,importFolderNodeId);
				}
			} else {
				logger.info("importFolder has no children");
			}
		} else {
			logger.info("no importFolder available");
		}
	}

	private HashMap<String, Object> getImportFolderProps() throws Throwable {
		String repositoryRoot = null;
		try {
			repositoryRoot = mcAlfrescoBaseClient.getRepositoryRoot();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}

		String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
		HashMap<String, Object> importFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
				OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
		return importFolderProps;
	}

	public String safe(Map newNodeProps, String cursor, String set) throws Throwable {
		logger.info("called cursor:" + cursor + " set:" + set);

		String replicationId = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
		String lomCatalogId = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
		String timestamp = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);

		if (replicationId == null) {
			logger.error("no replicationId in newNodeProps provided. will not safe/modify record.");
			return null;
		}

		if (lomCatalogId == null) {
			logger.error("no lomCatalogId in newNodeProps provided. will not safe/modify record.");
			return null;
		}

		// check if importfolder exsists
		String importFolderId = null;

		String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
		HashMap<String, Object> importFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
				OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
		if (importFolderProps == null) {
			HashMap newimportFolderProps = new HashMap();
			newimportFolderProps.put(CCConstants.CM_NAME, OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
			newimportFolderProps.put(CCConstants.CM_PROP_C_TITLE, OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
			importFolderId = mcAlfrescoBaseClient.createNode(companyHomeId, CCConstants.CCM_TYPE_MAP, newimportFolderProps);
		} else {
			importFolderId = (String) importFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
		}

		String oaiImportBasefolder = importFolderId;

		if (set == null || set.trim().equals("")) {
			set = "unknownset";
		}

		// replace evil chars
		set = set.replace(":", "_");

		HashMap<String, Object> setChild = mcAlfrescoBaseClient.getChild(importFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, set);
		if (setChild == null) {
			HashMap newimportFolderProps = new HashMap();
			newimportFolderProps.put(CCConstants.CM_NAME, set);
			newimportFolderProps.put(CCConstants.CM_PROP_C_TITLE, set);
			importFolderId = mcAlfrescoBaseClient.createNode(importFolderId, CCConstants.CCM_TYPE_MAP, newimportFolderProps);
		} else {
			importFolderId = (String) setChild.get(CCConstants.SYS_PROP_NODE_UID);
		}

		if (cursor == null || cursor.trim().equals("")) {
			cursor = "last";
		}
		HashMap<String, Object> cursorChild = mcAlfrescoBaseClient.getChild(importFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, cursor);
		if (cursorChild == null) {
			HashMap newimportFolderProps = new HashMap();
			newimportFolderProps.put(CCConstants.CM_NAME, cursor);
			newimportFolderProps.put(CCConstants.CM_PROP_C_TITLE, cursor);
			importFolderId = mcAlfrescoBaseClient.createNode(importFolderId, CCConstants.CCM_TYPE_MAP, newimportFolderProps);
		} else {
			importFolderId = (String) cursorChild.get(CCConstants.SYS_PROP_NODE_UID);
		}

		// watch out if object was already imported
		// HashMap<String, HashMap<String, Object>> alfResult =
		// mcAlfrescoBaseClient.search("@ccm\\:replicationsourceid:"+replicationId+" AND @ccm\\:replicationsource:"+lomCatalogId,
		// CCConstants.CCM_TYPE_IO);
		// HashMap searchProps = new HashMap();
		// searchProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, lomCatalogId);
		// searchProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, replicationId);
		// HashMap<String,Object> childProps = mcAlfrescoBaseClient.getChildRecursive(oaiImportBasefolder, CCConstants.CCM_TYPE_IO, searchProps);
		HashMap<String, Object> childProps = getPropsIfExsists(lomCatalogId, replicationId, oaiImportBasefolder);

		if (childProps != null) {

			// update
			logger.info("found one local Object for: Id:" + replicationId + " catalog:" + lomCatalogId + " childProps:"
					+ childProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE) + " " + childProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID)
					+ " " + childProps.get(CCConstants.SYS_PROP_NODE_UID));
			HashMap<String, Object> oldProps = childProps;
			String oldTimeStamp = (String) oldProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);
			String newTimeStamp = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);

			String oldLicenseValid = (String) oldProps.get(CCConstants.CCM_PROP_IO_LICENSE_VALID);
			String newLicenseValid = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_LICENSE_VALID);
			boolean licenseValidChanged = false;
			if ((oldLicenseValid == null && newLicenseValid != null) || (oldLicenseValid != null && !oldLicenseValid.equals(newLicenseValid))) {
				licenseValidChanged = true;
			}

			boolean nodeMustBeUpdated = false;
			if (newTimeStamp != null && oldTimeStamp != null) {
				Date newDate = null;
				try {
					newDate = sdf.parse(newTimeStamp);
					Date oldDate = sdf.parse(oldTimeStamp);

					if (newDate.after(oldDate)) {
						nodeMustBeUpdated = true;
					}

				} catch (ParseException e) {
					logger.error(e.getMessage() + " while comparing old and new timestamp for id:" + replicationId + " oldTimeStamp:" + oldTimeStamp
							+ " newTimeStamp:" + newTimeStamp);

					// if old date was damaged but new date is ok
					if (newDate != null) {
						nodeMustBeUpdated = true;
					}

				}
			}

			logger.info("oldTimeStamp:" + oldTimeStamp + " newTimeStamp:" + newTimeStamp);
			if (nodeMustBeUpdated) {
				// @TODO update only when timestamp changed
				// updateNode(alfResult.keySet().iterator().next(),newNodeProps);
				logger.info(" newTimeStamp is after oldTimeStamp have to update object id:" + replicationId);
				updateNode((String) childProps.get(CCConstants.SYS_PROP_NODE_UID), newNodeProps);
				setModifiedDate((String) childProps.get(CCConstants.SYS_PROP_NODE_UID), newNodeProps);
			} else if (licenseValidChanged) {
				logger.info(" license valid changed. have to update object. oldLicenseValid:" + oldLicenseValid + " newLicenseValid:"
						+ newLicenseValid);
				updateNode((String) childProps.get(CCConstants.SYS_PROP_NODE_UID), newNodeProps);
				setModifiedDate((String) childProps.get(CCConstants.SYS_PROP_NODE_UID), newNodeProps);
			} else {
				logger.info(" newTimeStamp.equals(oldTimeStamp) I'll do nothing");
			}
			return (String) childProps.get(CCConstants.SYS_PROP_NODE_UID);

		} else {
			// insert
			String nodeId;
			logger.info("found no local Object for: Id:" + replicationId + " catalog:" + lomCatalogId + " creating new one");
			try{			
				nodeId=createNode(importFolderId, CCConstants.CCM_TYPE_IO, CCConstants.CM_ASSOC_FOLDER_CONTAINS, newNodeProps);
			
			}catch(org.alfresco.service.cmr.repository.DuplicateChildNodeNameException e){
				String name = (String)newNodeProps.get(CCConstants.CM_NAME);
				name = name + System.currentTimeMillis();
				newNodeProps.put(CCConstants.CM_NAME, name);
				nodeId=createNode(importFolderId, CCConstants.CCM_TYPE_IO, CCConstants.CM_ASSOC_FOLDER_CONTAINS, newNodeProps);
			}
			setModifiedDate(nodeId,newNodeProps);
			return nodeId;
			
		}
		
	}

	private synchronized void setModifiedDate(String nodeId,Map newNodeProps) throws NotSupportedException, SystemException, IllegalStateException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
		if(newNodeProps.containsKey(CCConstants.CM_PROP_C_MODIFIED)){
			NodeRef nodeRef=new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
			BehaviourFilter filter=(BehaviourFilter)AlfAppContextGate.getApplicationContext().getBean("policyBehaviourFilter");
			ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
			UserTransaction tx = serviceRegistry.getTransactionService().getUserTransaction();
			tx.begin();
			filter.disableBehaviour(nodeRef,ContentModel.ASPECT_AUDITABLE);

			mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.CM_PROP_C_MODIFIED,(String) newNodeProps.get(CCConstants.CM_PROP_C_MODIFIED));
			filter.enableBehaviour(nodeRef,ContentModel.ASPECT_AUDITABLE);
			tx.commit();
		}
	}

	private HashMap<String, Object> getPropsIfExsists(String replSource, String replSourceId, String importFolderId) throws Throwable {
		HashMap<String, HashMap<String, Object>> allNodesInImpFolder = this.getAllNodesInImportfolder(importFolderId);
		if (allNodesInImpFolder != null) {
			for (Map.Entry<String, HashMap<String, Object>> entry : allNodesInImpFolder.entrySet()) {
				HashMap<String, Object> props = entry.getValue();
				String currentReplSource = (String) props.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
				String currentReplSourceId = (String) props.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
				if (replSource.equals(currentReplSource) && replSourceId.equals(currentReplSourceId)) {
					return props;
				}
			}
		}
		return null;
	}

	public HashMap<String, HashMap<String, Object>> getAllNodesInImportfolder() throws Throwable {
		HashMap<String, Object> importFolderProps = getImportFolderProps();
		if (importFolderProps != null) {
			String importFolderNodeId = (String) importFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
			return this.getAllNodesInImportfolder(importFolderNodeId);
		} else {
			logger.info("returns importFolderProps == null");
			return null;
		}

	}

	public HashMap<String, HashMap<String, Object>> getAllNodesInImportfolder(String importFolderId) throws Throwable {
		if (allNodesInImportfolder == null) {
			logger.info("allNodesInImportfolder is null starting to initialize it");
			allNodesInImportfolder = mcAlfrescoBaseClient.getChildrenRecursive(importFolderId, CCConstants.CCM_TYPE_IO);

			logger.info("allNodesInImportfolder initialize finished! size:" + ((allNodesInImportfolder != null) ? allNodesInImportfolder.size() : 0));
		}
		return allNodesInImportfolder;
	}

	public void updateNode(String nodeId, Map props) throws Throwable {
		// idea first delete all childs and create them new
		HashMap children = mcAlfrescoBaseClient.getChildren(nodeId);
		for (Object key : children.keySet()) {
			mcAlfrescoBaseClient.removeNode((String) key, nodeId,false);
		}

		HashMap<String, Object> simpleProps = new HashMap<String, Object>();
		HashMap<String, Object> nodeProps = new HashMap<String, Object>();
		for (Object key : props.keySet()) {
			String propKey = (String) key;
			if (propKey.startsWith("TYPE#")) {
				nodeProps.put(propKey, props.get(propKey));
			} else {
				simpleProps.put(propKey, props.get(propKey));
			}
		}

		mcAlfrescoBaseClient.updateNode(nodeId, simpleProps);
		for (Object key : nodeProps.keySet()) {
			String typekey = (String) key;
			String[] splitted = typekey.split("#");
			if (splitted != null && splitted.length == 3) {
				String subNodeType = splitted[1];
				String subNodeAssociation = splitted[2];
				if (nodeProps.get(typekey) instanceof List) {
					List list = (List) nodeProps.get(typekey);
					for (Object listentry : list) {
						Map subNodeProps = (Map) listentry;
						createNode(nodeId, subNodeType, subNodeAssociation, subNodeProps);
					}
				} else {
					// it must be a Map
					Map subNodeProps = (Map) nodeProps.get(typekey);
					createNode(nodeId, subNodeType, subNodeAssociation, subNodeProps);
				}
			}
		}

	}

	public String createNode(String parentId, String type, String association, Map props) throws Throwable {
		HashMap<String, Object> simpleProps = new HashMap<String, Object>();
		HashMap<String, Object> nodeProps = new HashMap<String, Object>();
		String[] aspects=null;
		for (Object key : props.keySet()) {
			String propKey = (String) key;
			if(propKey.equals("ASPECTS")){
				aspects=(String[])props.get(propKey);
			}
			else if (propKey.startsWith("TYPE#")) {
				nodeProps.put(propKey, props.get(propKey));
			} else {
				simpleProps.put(propKey, props.get(propKey));
			}
		}

		// CLEANAUP?
		/*
		 * System.out.println("type:"+type); for(Object key:simpleProps.keySet()){ System.out.println("key:"+key+" val:"+simpleProps.get(key)); }
		 */

		//simpleProps.remove("{http://www.alfresco.org/model/content/1.0}title");
		//simpleProps.remove("{http://www.alfresco.org/model/content/1.0}edu_metadataset");
		//simpleProps.remove("{http://www.campuscontent.de/model/1.0}objecttype");
		
		//bad:
		//simpleProps.remove("{http://www.campuscontent.de/model/lom/1.0}rights_description");
		//simpleProps.remove("{http://www.campuscontent.de/model/1.0}taxonentry");//cause it's null?
		
		//maybe bad
		/*simpleProps.remove("{http://www.campuscontent.de/model/lom/1.0}general_keyword");
		simpleProps.remove("{http://www.campuscontent.de/model/lom/1.0}title");
		simpleProps.remove("{http://www.campuscontent.de/model/lom/1.0}general_description");
		simpleProps.remove("{http://www.campuscontent.de/model/1.0}metadatacontributer_provider");
		simpleProps.remove("{http://www.campuscontent.de/model/1.0}metadatacontributer_creator");
		simpleProps.remove("{http://www.campuscontent.de/model/1.0}educationallearningresourcetype");*/
		
		//null props
		//simpleProps.remove("{http://www.campuscontent.de/model/1.0}taxonid");
		
		
		//simpleProps.remove("{http://www.campuscontent.de/model/1.0}educationalcontext");
	/*
		simpleProps.remove("{http://www.campuscontent.de/model/lom/1.0}general_language");
		
		simpleProps.remove("{http://www.campuscontent.de/model/1.0}replicationsourceid");	
		
		simpleProps.remove("{http://www.campuscontent.de/model/1.0}replicationsourcetimestamp");
		
		simpleProps.remove("{http://www.campuscontent.de/model/1.0}replicationsource");
	*/

		String newNodeId;
		// do not auto create versions (otherwise the node will get several versions e.g. during binary handler or preview)
		simpleProps.put(CCConstants.CCM_PROP_IO_CREATE_VERSION,false);
		try {
			newNodeId = mcAlfrescoBaseClient.createNode(parentId, type, association, simpleProps);
		}catch(DuplicateChildNodeNameException e) {
			simpleProps.put(CCConstants.CM_NAME, (String)simpleProps.get(CCConstants.CM_NAME) + System.currentTimeMillis());
			newNodeId = mcAlfrescoBaseClient.createNode(parentId, type, association, simpleProps);
		}
		if(aspects!=null){
			for(String aspect : aspects){
				mcAlfrescoBaseClient.addAspect(newNodeId, aspect);
			}
		}
		for (Object key : nodeProps.keySet()) {
			String typekey = (String) key;
			String[] splitted = typekey.split("#");
			if (splitted != null && splitted.length == 3) {
				String subNodeType = splitted[1];
				String subNodeAssociation = splitted[2];
				if (nodeProps.get(typekey) instanceof List) {
					List list = (List) nodeProps.get(typekey);
					for (Object listentry : list) {
						Map subNodeProps = (Map) listentry;
						createNode(newNodeId, subNodeType, subNodeAssociation, subNodeProps);
					}
				} else {
					// it must be a Map
					// logger.info("typekey:"+typekey);
					Map subNodeProps = (Map) nodeProps.get(typekey);
					createNode(newNodeId, subNodeType, subNodeAssociation, subNodeProps);
				}
			}
		}

		return newNodeId;
	}

	public HashMap<String, String> getReplicationIdTimestampMap() {
		if (replIdTimestampMap == null) {
			try {
				HashMap<String, HashMap<String, Object>> allNodes = getAllNodesInImportfolder();
				replIdTimestampMap = new HashMap<String, String>();
				for (Map.Entry<String, HashMap<String, Object>> entry : allNodes.entrySet()) {

					HashMap<String, Object> props = entry.getValue();

					String replId = (String) props.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
					String timestamp = (String) props.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);

					if (replId != null && !replId.trim().equals("") && timestamp != null && !timestamp.trim().equals("")) {
						replIdTimestampMap.put(replId, timestamp);
					} else {
						logger.error("cannot add nodeId " + entry.getKey() + " to replIdTimestampMap replId:" + replId + " timestamp:" + timestamp);
					}
				}

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return replIdTimestampMap;
	}

	/**
	 * checks if an repl object must be created or updated
	 * 
	 * @param replId
	 * @param timeStamp
	 * @return
	 */
	public boolean mustBePersisted(String replId, String timeStamp) {

		// we will not safe without replId
		if (replId == null) {
			return false;
		}
		
		String oldTimeStamp = getReplicationIdTimestampMap().get(replId);

		// we will not safe without timestamp
		if (timeStamp == null) {
			return false;
		}

		// does not exist
		if (oldTimeStamp == null) {
			return true;
		}

		Date newDate = null;
		try {
			newDate = sdf.parse(timeStamp);
			Date oldDate = sdf.parse(oldTimeStamp);

			if (newDate.after(oldDate)) {
				return true;
			}

		} catch (ParseException e) {
			logger.error(e.getMessage() + " while comparing old and new timestamp for id:" + replId + " oldTimeStamp:" + oldTimeStamp
					+ " newTimeStamp:" + timeStamp);

			// if old date was damaged but new date is ok
			if (newDate != null) {
				return true;
			}

		}
		return false;
	}
}
