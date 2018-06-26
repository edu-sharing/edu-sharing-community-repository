package org.edu_sharing.metadataset.v2.tools;

import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfo;

public class MetadataHelper {

	public static MetadataSetV2 getMetadataset(ApplicationInfo appId,String mdsSet) throws Exception{
		String locale="default";
		try{
			locale = new AuthenticationToolAPI().getCurrentLocale();
		}catch(Throwable t){}
		return MetadataReaderV2.getMetadataset(appId, mdsSet,locale);		
	}

}
