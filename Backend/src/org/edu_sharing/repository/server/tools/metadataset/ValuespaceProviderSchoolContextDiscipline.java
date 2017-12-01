package org.edu_sharing.repository.server.tools.metadataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.edu_sharing.repository.client.rpc.SchoolContextValues;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.schoolcontext.SchoolContextService;
import org.edu_sharing.service.schoolcontext.SchoolContextServiceImpl;

public class ValuespaceProviderSchoolContextDiscipline implements ValuespaceProvider {

	@Override
	public List<MetadataSetValueKatalog> getValuespace() {

		List<MetadataSetValueKatalog> result = new ArrayList<MetadataSetValueKatalog>();

		try {

			ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
			AuthenticationTool authToolAPI = RepoFactory.getAuthenticationToolInstance(homeRepo.getAppId());

			authToolAPI.createNewSession(homeRepo.getUsername(), homeRepo.getPassword());

			SchoolContextService scs = new SchoolContextServiceImpl(new MCAlfrescoAPIClient());

			SchoolContextValues vals = scs.getSchoolConextValues();
			HashMap<String, String> disciplineMap = vals.getSchoolSubject();

			for (Map.Entry<String, String> entry : disciplineMap.entrySet()) {
				MetadataSetValueKatalog mdsVK = new MetadataSetValueKatalog();
				mdsVK.setCaption(entry.getValue());
				mdsVK.setKey(entry.getKey());

				HashMap<String, String> i18n = new HashMap<String, String>();
				i18n.put(CCConstants.defaultLocale, entry.getValue());
				mdsVK.setI18n(i18n);

				result.add(mdsVK);
			}

		} catch (Throwable e) {
			e.printStackTrace();
		}

		return result;
	}

}
