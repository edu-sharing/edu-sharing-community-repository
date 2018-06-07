package org.edu_sharing.service.nodeservice;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetModelProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetModelType;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSets;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.context.ApplicationContext;

public class NodeServiceImpl implements org.edu_sharing.service.nodeservice.NodeService {
	
	/* Some safe properties they're not necessary in the mds, but the client is allowed to define */
	private static final String[] SAFE_PROPS = new String[]{
			CCConstants.LOM_PROP_TECHNICAL_FORMAT,
			CCConstants.CCM_PROP_IO_WWWURL,
			CCConstants.CCM_PROP_IO_CREATE_VERSION,
			CCConstants.CCM_PROP_IO_VERSION_COMMENT,
			CCConstants.CCM_PROP_CCRESSOURCETYPE,
			CCConstants.CCM_PROP_CCRESSOURCESUBTYPE,
			CCConstants.CCM_PROP_CCRESSOURCEVERSION,
			CCConstants.CCM_PROP_WF_INSTRUCTIONS,
			CCConstants.CCM_PROP_WF_PROTOCOL,
			CCConstants.CCM_PROP_WF_RECEIVER,
			CCConstants.CCM_PROP_WF_STATUS,
			CCConstants.CCM_PROP_MAP_COLLECTIONREMOTEID,
			CCConstants.CM_PROP_METADATASET_EDU_METADATASET,
			CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET,
			CCConstants.CCM_PROP_EDITOR_TYPE,
			CCConstants.CCM_PROP_TOOL_OBJECT_TOOLINSTANCEREF,
			CCConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY,
			CCConstants.CCM_PROP_SAVED_SEARCH_MDS,
			CCConstants.CCM_PROP_SAVED_SEARCH_QUERY,
			CCConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS,
			CCConstants.CCM_PROP_AUTHOR_FREETEXT,
			CCConstants.CCM_PROP_CHILDOBJECT_ORDER,
			CCConstants.CCM_PROP_LINKTYPE,
			};
	private static final String[] LICENSE_PROPS = new String[]{
			CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION,
			CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK,
			CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL,
			CCConstants.CCM_PROP_IO_LICENSE_PROFILE_URL,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED
	};
	String repositoryId = ApplicationInfoList.getHomeRepository().getAppId();
	MetadataSets metadataSets = RepoFactory.getMetadataSetsForRepository(repositoryId);
	private ServiceRegistry serviceRegistry = null;
	private NodeService nodeService = null;
	private ApplicationInfo application;
	
	Logger logger = Logger.getLogger(NodeServiceImpl.class);

	Repository repositoryHelper = null;

	MCAlfrescoAPIClient apiClient;
	
	public NodeServiceImpl(String appId) {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		nodeService = serviceRegistry.getNodeService();
		repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
		application=ApplicationInfoList.getRepositoryInfoById(appId);
		HashMap homeAuthInfo = null;
		if(!ApplicationInfoList.getRepositoryInfoById(repositoryId).ishomeNode()){
			homeAuthInfo = new AuthenticationToolAPI().getAuthentication(Context.getCurrentInstance().getRequest().getSession());
		}
		try{
			apiClient = (MCAlfrescoAPIClient) RepoFactory.getInstance(repositoryId, homeAuthInfo);
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		}
		
	}
	public void updateNode(String nodeId, HashMap<String, String[]> props) throws Throwable{
			String nodeType = getType(nodeId);
			String parentId = nodeService.getPrimaryParent(new NodeRef(Constants.storeRef,nodeId)).getParentRef().getId();
			HashMap<String,Object> toSafeProps = getToSafeProps(props,nodeType, parentId,null);
			updateNodeNative(nodeId, toSafeProps);
	}
	
	public NodeRef copyNode(String nodeId, String toNodeId, boolean copyChildren) throws Throwable {
		NodeRef nodeRef = new NodeRef(Constants.storeRef, nodeId);

		CopyService copyService = serviceRegistry.getCopyService();

		String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
		NodeRef copyNodeRef = copyService.copyAndRename(nodeRef, new NodeRef(Constants.storeRef, toNodeId), QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS),
				QName.createQName(name), copyChildren);
		resetVersion(copyNodeRef);
		return copyNodeRef;
	}
	
	private void resetVersion(NodeRef nodeRef) throws Throwable {
		if(CCConstants.CCM_TYPE_IO.equals(getType(nodeRef.getId()))) {
			HashMap<String, Object> props = new HashMap<String,Object>();
			props.put(CCConstants.LOM_PROP_LIFECYCLE_VERSION,"1.0");
			updateNodeNative(nodeRef.getId(), props);
		}
	}
	
	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props) throws Throwable{
		HashMap<String,Object> toSafeProps = getToSafeProps(props,nodeType ,parentId,null);
		return createNodeBasic(parentId, nodeType, toSafeProps);
	}
	
	@Override
	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props, String childAssociation)
			throws Throwable {
		HashMap<String,Object> toSafeProps = getToSafeProps(props,nodeType,parentId,null);
		return this.createNodeBasic(Constants.storeRef, parentId, nodeType,childAssociation, toSafeProps);
	}
	@Override
	public String createNodeBasic(String parentID, String nodeTypeString, HashMap<String, Object> _props) {
		return this.createNodeBasic(Constants.storeRef, parentID, nodeTypeString,CCConstants.CM_ASSOC_FOLDER_CONTAINS, _props);
	}
	@Override
	public String createNodeBasic(StoreRef store, String parentID, String nodeTypeString, String childAssociation, HashMap<String, Object> _props) {
		childAssociation = (childAssociation == null) ? CCConstants.CM_ASSOC_FOLDER_CONTAINS : childAssociation;
		Map<QName, Serializable> properties = transformPropMap(_props);

		NodeRef parentNodeRef = new NodeRef(store, parentID);
		QName nodeType = QName.createQName(nodeTypeString);

		String assocName = (String) _props.get(CCConstants.CM_NAME);
		if (assocName == null) {
			assocName = "defaultAssociationName";
		} else {

			// assco name must have be smaller than a maxlength
			// https://issues.alfresco.com/jira/browse/MNT-2417
			assocName = QName.createValidLocalName(assocName);
		}
		assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;

		ChildAssociationRef childRef = nodeService.createNode(parentNodeRef, QName.createQName(childAssociation), QName.createQName(assocName), nodeType,
				properties);
		return childRef.getChildRef().getId();
	}
	
	
	@Override
	public String getCompanyHome(){
		return repositoryHelper.getCompanyHome().getId();
	}
	HashMap<String,Object> getToSafeProps(HashMap<String, String[]> props, String nodeType, String parentId,String templateName) throws Throwable{
		String[] metadataSetIdArr = props.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
		
		String metadataSetId = (metadataSetIdArr != null && metadataSetIdArr.length > 0) ? metadataSetIdArr[0] : null;
		
		if(metadataSetId == null) {
			Boolean forceMds = false;
			try {
				forceMds = (Boolean)nodeService.getProperty(new NodeRef(MCAlfrescoAPIClient.storeRef,parentId), QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET));
				if(forceMds == null) forceMds = false;
			}catch(Throwable t) {}
			if(forceMds) {
				metadataSetId = (String)nodeService.getProperty(new NodeRef(MCAlfrescoAPIClient.storeRef,parentId), QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
			}
			else {
				if(HttpContext.getCurrentMetadataSet() != null && HttpContext.getCurrentMetadataSet().trim().length() > 0) {
					metadataSetId = HttpContext.getCurrentMetadataSet();
				}else {
					metadataSetId = CCConstants.metadatasetdefault_id;
				}
				props.put(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, new String[] {metadataSetId});
			}
		}
		
		MetadataSetV2 mds = MetadataReaderV2.getMetadataset(application, metadataSetId);
		HashMap<String,Object> toSafe = new HashMap<String,Object>();
		for (MetadataWidget widget : (templateName==null ? mds.getWidgetsByNodeType(nodeType) : mds.getWidgetsByTemplate(templateName))) {
			String id=widget.getId();
			if(!checkWidgetConditionTrue(widget)) {
				logger.info("widget "+id+" skipped because condition failed");
				logger.info("condition that should match: "+widget.getCondition().getType()+" "+(widget.getCondition().isNegate() ? "!=" : "=" )+" "+widget.getCondition().getValue());
				continue;
			}
			id=CCConstants.getValidGlobalName(id);
			String [] values = props.get(id);
			if("range".equals(widget.getType())){
				String [] valuesFrom = props.get(id+"_from");
				String [] valuesTo = props.get(id+"_to");
				if(valuesFrom==null || valuesTo==null)
					continue;
				toSafe.put(id+"_from",valuesFrom[0]);
				toSafe.put(id+"_to",valuesTo[0]);
			}
			else if("defaultvalue".equals(widget.getType())) {
				logger.info("will save property "+widget.getId()+" with predefined defaultvalue "+widget.getDefaultvalue());
				toSafe.put(id,widget.getDefaultvalue());
				continue;
			}
			if(values==null)
				continue;
			if(!widget.isMultivalue() && values.length>1)
				throw new IllegalArgumentException("Multiple values given for a non-multivalue widget: ID "+id+", widget type "+widget.getType());
			if(widget.isMultivalue()){
				toSafe.put(id,new ArrayList<String>(Arrays.asList(values)));
			}
			else{
				toSafe.put(id,values[0]);
			}
		}

		for(String property : getAllSafeProps()){
			if(!props.containsKey(property)) continue;
			
			String[] arr = props.get(property);
			if(arr != null){
				if(arr.length==0)
					toSafe.put(property,null);
				else if(arr.length > 1)
					toSafe.put(property,new ArrayList<String>(Arrays.asList(arr)));
				else
					toSafe.put(property, arr[0]);
			}
		}

		if (isSubOf(nodeType, CCConstants.CM_TYPE_OBJECT)) {
			// only when there is an title
			String[] cmNameReadableNameArr = (String[])props.get(CCConstants.CM_NAME);
			String cmNameReadableName = (cmNameReadableNameArr != null && cmNameReadableNameArr.length > 0)  ? cmNameReadableNameArr[0] : null;
			String titleProp = null;
			if(cmNameReadableName == null){
				String[] lomTitleArr =  (String[]) props.get(CCConstants.LOM_PROP_GENERAL_TITLE);
				cmNameReadableName = (lomTitleArr != null && lomTitleArr.length > 0) ? lomTitleArr[0] : null;
				titleProp = CCConstants.LOM_PROP_GENERAL_TITLE;
				if (cmNameReadableName == null) {
					String[] cmTitleArr = (String[]) props.get(CCConstants.CM_PROP_C_TITLE);
					cmNameReadableName = (cmTitleArr != null && cmTitleArr.length > 0) ? cmTitleArr[0] : null;
					titleProp = CCConstants.CM_PROP_C_TITLE;
				}
			}
			if (cmNameReadableName != null) {
	
				// replace ie fakepath like C:\fakepath\test.png
				String replaceFakePathPrefixRegEx = "^[A-Za-z]:\\\\fakepath\\\\";
				String fakePaceCleanedString = cmNameReadableName.replaceFirst(replaceFakePathPrefixRegEx, "");
				if (fakePaceCleanedString.length() > 0) {
					cmNameReadableName = fakePaceCleanedString;
				}
	
				cmNameReadableName = NodeServiceHelper.cleanupCmName(cmNameReadableName);
	
				toSafe.put(CCConstants.CM_NAME, cmNameReadableName);	
			}
		}
		
		return toSafe;
	}
	//transient Logger logger = Logger.getLogger(MetadataWidget.class);
	/** resolves this widget's condition
	 * only works for condition type TOOLPERMISSION
	 * @return
	 */
	private boolean checkWidgetConditionTrue(MetadataWidget widget) {
		MetadataWidget.Condition condition = widget.getCondition();
		if(widget.getCondition()==null)
			return true;
		if(MetadataWidget.Condition.CONDITION_TYPE.TOOLPERMISSION.equals(condition.getType())){
			boolean result=ToolPermissionServiceFactory.getInstance().hasToolPermission(condition.getValue());
			return result!=condition.isNegate();
		}
		//logger.info("skipping condition type "+condition.getType()+" for widget "+getId()+" since it's not supported in backend");
		return true;
	}
	private static Iterable<String> getAllSafeProps() {
		List<String> safe=new ArrayList<>();
		safe.addAll(Arrays.asList(SAFE_PROPS));
		safe.addAll(Arrays.asList(LICENSE_PROPS));
		safe.addAll(CCConstants.getLifecycleContributerPropsMap().values());
		safe.addAll(CCConstants.getMetadataContributerPropsMap().values());
		return safe;
	}
	@Override
	public HashMap<String, String[]> getNameProperty(String name) {
		HashMap<String, String[]> map=new HashMap<String, String[]>();
		map.put(CCConstants.CM_NAME, new String[]{name});
		return map;
	}
	
	@Override
	public String findNodeByName(String parentId, String name){
		List<ChildAssociationRef> children = nodeService.getChildAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId));
		for(ChildAssociationRef child : children){
			String childName=(String) nodeService.getProperty(child.getChildRef(), QName.createQName(CCConstants.CM_NAME));
			if(childName.equals(name))
				return child.getChildRef().getId();
		}
		return null;
	}
	
	public HashMap<String, HashMap<String, Object>> getChildrenByType(StoreRef store, String nodeId, String type) {
		HashMap<String, HashMap<String, Object>> result = new HashMap<String, HashMap<String, Object>>();
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(new NodeRef(store, nodeId));
		for (ChildAssociationRef child : childAssocList) {

			String childType = nodeService.getType(child.getChildRef()).toString();
			if (childType.equals(type)) {

				HashMap<String, Object> resultProps = getPropertiesWithoutChildren(child.getChildRef());
				String childNodeId = child.getChildRef().getId();
				result.put(childNodeId, resultProps);

			}
		}
		return result;
	}
	
	public HashMap<String, Object> getChild(StoreRef store, String parentId, String type, String property, String value) {
		HashMap<String, HashMap<String, Object>> children = this.getChildrenByType(store, parentId, type);
		for (String childNodeId : children.keySet()) {
			HashMap<String, Object> childProps = children.get(childNodeId);
			String propValue = (String) childProps.get(property);
			if (propValue != null && propValue.equals(value))
				return childProps;
		}
		return null;
	}
	
	private HashMap<String, Object> getPropertiesWithoutChildren(NodeRef nodeRef) {
		Map<QName, Serializable> childPropMap = nodeService.getProperties(nodeRef);
		HashMap<String, Object> resultProps = new HashMap<String, Object>();

		String nodeType = nodeService.getType(nodeRef).toString();

		for (QName qname : childPropMap.keySet()) {

			Serializable object = childPropMap.get(qname);

			String metadataSetId = (String) childPropMap.get(QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));

			String value = formatData(nodeType, qname.toString(), object, metadataSetId);
			resultProps.put(qname.toString(), value);

			// VCard
			String type = nodeService.getType(nodeRef).toString();
			HashMap<String, Object> vcard = VCardConverter.getVCardHashMap(type, qname.toString(), value);
			if (vcard != null && vcard.size() > 0)
				resultProps.putAll(vcard);

		}

		resultProps.put(CCConstants.REPOSITORY_ID, repositoryId);
		resultProps.put(CCConstants.REPOSITORY_CAPTION, ApplicationInfoList.getHomeRepository().getAppCaption());

		return resultProps;
	}
	
	public String formatData(String type, String key, Object value, String metadataSetId) {
		String returnValue = null;
		if (key != null && value != null) {

			// value is date than put a String with a long value so that it can
			// be formated with userInfo later
			if (value instanceof Date) {

				Date date = (Date) value;

				if (date != null) {
					returnValue = new Long(date.getTime()).toString();
				}

			} else {
				returnValue = getValue(type, key, value, metadataSetId);
			}
			
			// like de_DE=null in gui
			if (returnValue == null && value != null && !(value instanceof MLText || value instanceof List)) {
				returnValue = value.toString();
			}
		}
		return returnValue;
	}
	
	protected String getValue(String type, String prop, Object _value, String metadataSetId) {

		MetadataSetModelProperty mdsmProp = getMetadataSetModelProperty(metadataSetId, type, prop);

		if (_value instanceof List && ((List) _value).size() > 0) {
			String result = null;
			for (Object value : (List) _value) {
				if (result != null)
					result += CCConstants.MULTIVALUE_SEPARATOR;
				if (value != null) {
					if (value instanceof MLText) {
						String tmpStr = getMLTextString(value, mdsmProp);
						if (result != null)
							result += tmpStr;
						else
							result = tmpStr;
					} else {
						if (result != null)
							result += value.toString();
						else
							result = value.toString();
					}
				}
			}

			return result;
		} else if (_value instanceof List && ((List) _value).size() == 0) {
			// cause empty list toString returns "[]"
			return "";
		} else if (_value instanceof String) {
			return (String) _value;
		} else if (_value instanceof Number) {
			return _value.toString();
		} else if (_value instanceof MLText) {
			return getMLTextString(_value, mdsmProp);
		} else {
			return _value.toString();
		}

	}

	MetadataSetModelProperty getMetadataSetModelProperty(String metadataSetId, String type, String prop) {
		MetadataSetModelProperty mdsmProp = null;

		// test take the deafault metadataset when metadatasetId is null
		if (metadataSetId == null)
			metadataSetId = CCConstants.metadatasetdefault_id;

		if (metadataSetId != null) {
	
			MetadataSet mds = metadataSets.getMetadataSetById(metadataSetId);
			if (mds != null) {
				MetadataSetModelType mdsmt = mds.getMetadataSetModelType(type);
				if (mdsmt != null) {
					mdsmProp = mdsmt.getMetadataSetModelProperty(prop);
				}
			}
		}
		return mdsmProp;
	}
	
	
	protected String getMLTextString(Object _mlText, MetadataSetModelProperty mdsmp) {

		if (_mlText instanceof MLText) {

			MLText mlText = (MLText) _mlText;

			if (mdsmp == null || (mdsmp != null && !mdsmp.getMultilang())) {
				return mlText.getDefaultValue();
			}

			String mlValueString = null;

			for (Locale locale : mlText.getLocales()) {
				String mlValue = mlText.getValue(locale);

				String localeStr = (locale.toString().equals(".default")) ? CCConstants.defaultLocale : locale.toString();

				if (mlValueString == null) {
					// for props that are declared multilang in alfresco model
					// but not in cc metadataset then props are saved as default.
					if (mlText.getLocales().size() == 1 && localeStr.equals(CCConstants.defaultLocale)) {
						mlValueString = mlValue;
					} else {
						mlValueString = localeStr + "=" + mlValue;
					}
				} else {
					mlValueString += "[,]" + localeStr + "=" + mlValue;
				}
			}
			if (mlValueString != null && !mlValueString.trim().equals("") && !mlValueString.contains(CCConstants.defaultLocale)) {
				mlValueString += "[,]default=" + mlText.getDefaultValue();
			}

			return mlValueString;
		} else {
			return _mlText.toString();
		}
	}
	
	@Override
	public void updateNodeNative(String nodeId, HashMap<String, Object> _props) {
		this.updateNodeNative(Constants.storeRef, nodeId, _props);
	}

	public void updateNodeNative(StoreRef store, String nodeId, HashMap<String, Object> _props) {

		try {
			Map<QName, Serializable> props = transformPropMap(_props);
			NodeRef nodeRef = new NodeRef(store, nodeId);

			// don't do this cause it's slow:
			/*
			 * for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
			 * nodeService.setProperty(nodeRef, entry.getKey(),
			 * entry.getValue()); }
			 */

			// prevent overwriting of properties that don't come with param _props
			Set<QName> changedProps = props.keySet();
			Map<QName, Serializable> currentProps = nodeService.getProperties(nodeRef);
			for (Map.Entry<QName, Serializable> entry : currentProps.entrySet()) {
				if (!changedProps.contains(entry.getKey())) {
					props.put(entry.getKey(), entry.getValue());
				}
			}

			nodeService.setProperties(nodeRef, props);

		} catch (org.hibernate.StaleObjectStateException e) {
			// this occurs sometimes in workspace
			// it seems it is an alfresco bug:
			// https://issues.alfresco.com/jira/browse/ETHREEOH-2461
			logger.error("Thats maybe an alfreco bug: https://issues.alfresco.com/jira/browse/ETHREEOH-2461", e);
		} catch (org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException e) {
			// this occurs sometimes in workspace
			// it seems it is an alfresco bug:
			// https://issues.alfresco.com/jira/browse/ETHREEOH-2461
			logger.error("Thats maybe an alfreco bug: https://issues.alfresco.com/jira/browse/ETHREEOH-2461", e);
		}

	}
	
	Map<QName, Serializable> transformPropMap(HashMap map) {
		Map<QName, Serializable> result = new HashMap<QName, Serializable>();
		for (Object key : map.keySet()) {

			try {
				Object value = map.get(key);
				if (value instanceof HashMap) {
					value = getMLText((HashMap) value);
				} else if (value instanceof List) {
					List transformedList = new ArrayList();
					for (Object valCol : (ArrayList) value) {
						if (valCol instanceof HashMap) {
							transformedList.add(getMLText((HashMap) valCol));
						} else {
							transformedList.add(valCol);
						}
					}
					value = transformedList;
				}
				result.put(QName.createQName((String) key), (Serializable) value);
			} catch (ClassCastException e) {
				logger.error("this prop has a wrong value:" + key + " val:" + map.get(key));
				logger.error(e.getMessage(), e);
			}
		}
		return result;
	}
	
	private MLText getMLText(HashMap i18nMap) {
		MLText mlText = new MLText();
		for (Object obj : i18nMap.keySet()) {
			String locale = (String) obj;
			mlText.addValue(new Locale(locale), (String) i18nMap.get(obj));
		}
		return mlText;
	}
	
	@Override
	public String getType(String nodeId) {
		return nodeService.getType(new NodeRef(Constants.storeRef, nodeId)).toString();
	}
	
	public ChildAssociationRef getParent(NodeRef nodeRef){
		return nodeService.getPrimaryParent(nodeRef);
	}
	
	public boolean isSubOf(String type, String parentType) throws Throwable {

		boolean isSubOf = serviceRegistry.getDictionaryService().isSubClass(QName.createQName(type), QName.createQName(parentType));
		return isSubOf;
	}
	
	public void setOwner(String nodeId, String username) {
		serviceRegistry.getOwnableService().setOwner(new NodeRef(Constants.storeRef, nodeId), username);
	}
	
	
	/**
	 * set's permission for one authority, leaves permissions already set for the authority
	 * 
	 * check ToolPermissions in the callers 
	 */
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission) throws Exception {

		PermissionService permissionsService = this.serviceRegistry.getPermissionService();
		NodeRef nodeRef = new NodeRef(Constants.storeRef, nodeId);
		if (inheritPermission != null) {
			logger.info("setInheritParentPermissions " + inheritPermission);
			permissionsService.setInheritParentPermissions(nodeRef, inheritPermission);
		}

		if (permissions != null) {
			for (String permission : permissions) {
				permissionsService.setPermission(new NodeRef(Constants.storeRef, nodeId), authority, permission, true);
			}
		}

	}
	@Override
	public String getOrCreateUserInbox() {
		NodeRef userhome=repositoryHelper.getUserHome(repositoryHelper.getPerson());
		List<ChildAssociationRef> inbox = nodeService.getChildAssocsByPropertyValue(userhome, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE), CCConstants.CCM_VALUE_MAP_TYPE_USERINBOX);
		if(inbox!=null && inbox.size()>0)
			return inbox.get(0).getChildRef().getId();
		HashMap<String,Object> properties=new HashMap<>();
		properties.put(CCConstants.CM_NAME,"Inbox");
		properties.put(CCConstants.CCM_PROP_MAP_TYPE,CCConstants.CCM_VALUE_MAP_TYPE_USERINBOX);		
		return createNodeBasic(userhome.getId(),CCConstants.CCM_TYPE_MAP,properties);
	}
	@Override
	public String getOrCreateUserSavedSearch() {
		NodeRef userhome=repositoryHelper.getUserHome(repositoryHelper.getPerson());
		List<ChildAssociationRef> savedSearch = nodeService.getChildAssocsByPropertyValue(userhome, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE), CCConstants.CCM_VALUE_MAP_TYPE_USERSAVEDSEARCH);
		if(savedSearch!=null && savedSearch.size()>0)
			return savedSearch.get(0).getChildRef().getId();
		HashMap<String,Object> properties=new HashMap<>();
		properties.put(CCConstants.CM_NAME,"SavedSearch");
		properties.put(CCConstants.CCM_PROP_MAP_TYPE,CCConstants.CCM_VALUE_MAP_TYPE_USERSAVEDSEARCH);		
		return createNodeBasic(userhome.getId(),CCConstants.CCM_TYPE_MAP,properties);
	}
	
	@Override
	public List<ChildAssociationRef> getChildrenChildAssociationRef(String parentID){
		if (parentID == null) {

			String startParentId = apiClient.getRootNodeId();
			if (startParentId == null || startParentId.trim().equals("")) {
				parentID = nodeService.getRootNode(Constants.storeRef).getId();
			} else {
				parentID = startParentId;
			}
		}

		NodeRef parentNodeRef = new NodeRef(Constants.storeRef, parentID);
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(parentNodeRef);
		return childAssocList;
	}
	@Override
	public List<ChildAssociationRef> getChildrenChildAssociationRef(String parentID,String childType){
		if (parentID == null) {

			String startParentId = apiClient.getRootNodeId();
			if (startParentId == null || startParentId.trim().equals("")) {
				parentID = nodeService.getRootNode(Constants.storeRef).getId();
			} else {
				parentID = startParentId;
			}
		}

		NodeRef parentNodeRef = new NodeRef(Constants.storeRef, parentID);
		if(childType==null) {
			return nodeService.getChildAssocs(parentNodeRef);
		}
		else {
			return nodeService.getChildAssocs(parentNodeRef,new HashSet<>(Arrays.asList(QName.createQName(childType))));
		}

	}
	public void createVersion(String nodeId, HashMap _properties) throws Exception{
		apiClient.createVersion(nodeId, _properties);
	}
	
	@Override
	public void writeContent(StoreRef store, String nodeID, InputStream content, String mimetype, String _encoding,
			String property) throws Exception {
		apiClient.writeContent(store, nodeID, content, mimetype, _encoding, property);
	}
	
	@Override
	public void removeNode(String nodeID, String fromID){
		this.removeNode(nodeID, fromID, true);
	}
	
	@Override
	public void removeNode(String nodeId, String parentId, boolean recycle) {
		apiClient.removeNode(nodeId, parentId, recycle);
	}
	
	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable{
		return apiClient.getProperties(storeProtocol, storeId, nodeId);
	}
	
	@Override
	public String getProperty(String storeProtocol, String storeId, String nodeId, String property) {
		return apiClient.getProperty(new StoreRef(storeProtocol,storeId), nodeId, property);
	}

	@Override
	public String getTemplateNode(String nodeId,boolean create) throws Throwable {
		if(!getType(nodeId).equals(CCConstants.CCM_TYPE_MAP) && !getType(nodeId).equals(CCConstants.CM_TYPE_FOLDER)){
			throw new IllegalArgumentException("Setting templates for nodes is only supported for type "+CCConstants.CCM_TYPE_MAP);
		}

		QName qname=QName.createQName(CCConstants.CCM_ASSOC_METADATA_PRESETTING_TEMPLATE);
		List<AssociationRef> result = nodeService.getTargetAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),qname);
		if(result!=null && result.size()>0)
			return result.get(0).getTargetRef().getId();
		if(!create)
			return null;

		addAspect(nodeId,CCConstants.CCM_ASPECT_METADATA_PRESETTING);
		HashMap<String,String[]> props = new HashMap<>();
		props.put(CCConstants.CM_NAME,new String[]{CCConstants.TEMPLATE_NODE_NAME});
		String id=createNode(nodeId,CCConstants.CCM_TYPE_IO,props);
		nodeService.createAssociation(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,id),
				qname);
		addAspect(id,CCConstants.CCM_ASPECT_METADATA_PRESETTING_TEMPLATE);
		return id;
	}
	@Override
	public void setTemplateProperties(String nodeId, HashMap<String, String[]> props) throws Throwable {
		//updateNode(getOrCreateTemplateNode(nodeId),props);
		String template = getTemplateNode(nodeId,true);
		String nodeType = getType(template);
		HashMap<String,Object> toSafeProps = getToSafeProps(props,nodeType, nodeId,"io_template");
		updateNodeNative(template, toSafeProps);
	}

	@Override
	public void setTemplateStatus(String nodeId, Boolean enable) throws Throwable{
		addAspect(nodeId,CCConstants.CCM_ASPECT_METADATA_PRESETTING);
		nodeService.setProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),
				QName.createQName(CCConstants.CCM_PROP_METADATA_PRESETTING_STATUS),
				enable);

	}

	@Override
	public InputStream getContent(String storeProtocol, String storeId, String nodeId,String contentProp) throws Throwable{
		return apiClient.getContent(nodeId,contentProp);
	}
	@Override
	public void addAspect(String nodeId, String aspect) {
		apiClient.addAspect(nodeId, aspect);
	}
	
	@Override
	public void removeAspect(String nodeId, String aspect) {
		apiClient.removeAspect(nodeId, aspect);
	}
	@Override
	public void removeProperty(String storeProtocol, String storeId, String nodeId, String property) {
		nodeService.removeProperty(new NodeRef(new StoreRef(storeProtocol,storeId),nodeId),QName.createQName(property));
	}
	@Override
	public String[] getAspects(String storeProtocol, String storeId, String nodeId){
		return apiClient.getAspects(new NodeRef(new StoreRef(storeProtocol,storeId),nodeId));
	}
	
	@Override
	public void moveNode(String newParentId, String childAssocType, String nodeId) {
		try{
			apiClient.moveNode(newParentId, childAssocType, nodeId);
		}catch(Exception e){
			throw new RuntimeException(e);
		}	
	}
	
	@Override
	public void revertVersion(String nodeId, String verLbl) throws Exception {
		try{
			apiClient.revertVersion(nodeId, verLbl);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public HashMap<String, HashMap<String, Object>> getVersionHistory(String nodeId) throws Exception {
		try{
			return apiClient.getVersionHistory(nodeId);
		}catch(Throwable e){
			throw new Exception(e);
		}
	}
	@Override
	public String importNode(String nodeId,String localParent) throws Throwable {
		throw new Exception("Not supported for local repository");
	}
	
	@Override
	public User getOwner(String storeId, String storeProtocol, String nodeId){
		return apiClient.getOwner(storeId, storeProtocol, nodeId);
	}
	
	@Override
	public void removeNode(String protocol, String store, String nodeId) {
		apiClient.removeNode(new StoreRef(protocol, store), nodeId);		
	}
	@Override
	public boolean exists(String protocol, String store, String nodeId) {
		return nodeService.exists(new NodeRef(new StoreRef(protocol, store), nodeId));
	}
}
