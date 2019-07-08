package org.edu_sharing.metadataset.v2.tools;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;

import java.util.Arrays;
import java.util.List;

public class MetadataHelper {

	public static MetadataSetV2 getMetadataset(ApplicationInfo appId,String mdsSet) throws Exception{
		return MetadataReaderV2.getMetadataset(appId, mdsSet,getLocale());
	}
	public static MetadataSetV2 getMetadataset(NodeRef node) throws Exception{
		String mdsSet = NodeServiceHelper.getProperty(node, CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
		if(mdsSet==null || mdsSet.isEmpty())
			mdsSet=CCConstants.metadatasetdefault_id;

		return MetadataReaderV2.getMetadataset(ApplicationInfoList.getHomeRepository(), mdsSet,getLocale());
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

}
