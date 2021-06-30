package org.edu_sharing.metadataset.v2.tools;

import io.swagger.config.ConfigFactory;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.admin.v1.Application;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MetadataHelper {

	public static MetadataSetV2 getLocalDefaultMetadataset() throws Exception{
		return MetadataReaderV2.getMetadataset(ApplicationInfoList.getHomeRepository(),CCConstants.metadatasetdefault_id,getLocale());
	}
	public static MetadataSetV2 getMetadataset(ApplicationInfo appId,String mdsSet) throws Exception{
		return MetadataReaderV2.getMetadataset(appId, mdsSet,getLocale());
	}
	public static MetadataSetV2 getMetadataset(NodeRef node) throws Exception{
		String mdsSet = NodeServiceHelper.getProperty(node, CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
		if(mdsSet==null || mdsSet.isEmpty())
			mdsSet=CCConstants.metadatasetdefault_id;

		return MetadataReaderV2.getMetadataset(ApplicationInfoList.getHomeRepository(), mdsSet,getLocale());
	}
	public static MetadataSetV2 getMetadataset(org.edu_sharing.restservices.shared.NodeRef node) throws Exception{
		if(node.isHomeRepo()) {
			return getMetadataset(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node.getId()));
		} else {
			return MetadataReaderV2.getMetadataset(ApplicationInfoList.getRepositoryInfoById(node.getRepo()), CCConstants.metadatasetdefault_id, getLocale());
		}
	}
	public static List<MetadataWidget> getWidgetsByNode(NodeRef node) throws Exception{
		MetadataSetV2 metadata = getMetadataset(node);
		return metadata.getWidgetsByNode(NodeServiceFactory.getLocalService().getType(node.getId()),Arrays.asList(NodeServiceHelper.getAspects(node)));
	}

		private static String getLocale() {
		String locale="default";
		try{
			locale = new AuthenticationToolAPI().getCurrentLocale();
		}catch(Throwable t){}
		return locale;
	}
	public static String getTranslation(String key) throws Exception {
		return getTranslation(ApplicationInfoList.getHomeRepository(),key,null);
	}
	public static String getTranslation(ApplicationInfo appId,String key,String fallback) throws Exception {
		return MetadataReaderV2.getTranslation(getMetadataset(appId,CCConstants.metadatasetdefault_id).getI18n(),key,fallback,getLocale());
	}

	public static String[] getDisplayNames(MetadataSetV2 mds, String key, Serializable value){
		try{
			if(mds == null){
				return null;
			}
			MetadataWidget widget = mds.findWidget(key);
			if(widget != null) {
				Map<String, MetadataKey> map = widget.getValuesAsMap();
				if (!map.isEmpty()) {
					String[] keys = ValueTool.getMultivalue((String) value);
					String[] values = new String[keys.length];
					for (int i = 0; i < keys.length; i++)
						values[i] = map.containsKey(keys[i]) ? map.get(keys[i]).getCaption() : keys[i];
					return values;
				}
			}
		} catch (Exception e){};
		return null;
	}

    /** resolves this widget's condition
     * only works for condition type TOOLPERMISSION
     * @return
     */
    public static boolean checkConditionTrue(MetadataCondition condition) {
        if(condition==null)
            return true;
        if(MetadataCondition.CONDITION_TYPE.TOOLPERMISSION.equals(condition.getType())){
            boolean result= ToolPermissionServiceFactory.getInstance().hasToolPermission(condition.getValue());
            return result!=condition.isNegate();
        }
        //logger.info("skipping condition type "+condition.getType()+" for widget "+getId()+" since it's not supported in backend");
        return true;
    }
}
