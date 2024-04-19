package org.edu_sharing.service.license;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.URLTool;

public class LicenseService {

	public String getIconUrl(String license,boolean dynamic){
		if(license==null || license.isEmpty())
			license="none";
		String ccImageName = license.toLowerCase().replace("_", "-")+".svg";
		String url = URLTool.getBaseUrl(dynamic) + "/ccimages/licenses/" + ccImageName;

		return url;
	}
	public String getIconUrl(String license){
		return getIconUrl(license,true);
	}

	public String getLicenseUrl(String license, String locale){
		return getLicenseUrl(license, locale, null);
	}

	public String getLicenseUrl(String license, String locale, String version){ return getLicenseUrl(license, locale, version, null); }
	
	public String getLicenseUrl(String license, String locale, String version,String jurisdiction){
		if(license==null || license.isEmpty())
			return null;

		version = (version == null || version.isEmpty()) ? "3.0" : version;
		locale = (locale == null || locale.isEmpty()) ? "en" : locale.split("_")[0];

		// If no jurisdiction is given we use the empty string
		// if version is 4.0 (international) we dont need a jurisdiction
		if (jurisdiction == null || jurisdiction.isEmpty() || version.equals("4.0"))
			jurisdiction = "";

		String result = null;
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_PDM)) {
			result = CCConstants.COMMON_LICENSE_CC_PDM_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_ZERO)) {
			result = CCConstants.COMMON_LICENSE_CC_ZERO_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_NC)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_NC_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_NC_ND)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_NC_ND_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_NC_SA)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_NC_SA_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_ND)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_ND_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_CC_BY_SA)) {
			result = CCConstants.COMMON_LICENSE_CC_BY_SA_LINK;
		}
		if (license.equals(CCConstants.COMMON_LICENSE_EDU_NC)) {
			result = CCConstants.COMMON_LICENSE_EDU_LINK;
		}

		if (license.equals(CCConstants.COMMON_LICENSE_EDU_NC_ND)) {
			result = CCConstants.COMMON_LICENSE_EDU_LINK;
		}

		if(result != null){
			if(result.contains("${version}")){
				result = result.replace("${version}", version);
			}

			if (result.contains("${jurisdiction}")) {
				String replacement = jurisdiction.isEmpty() ? "" : jurisdiction.toLowerCase() + "/";
				result = result.replace("${jurisdiction}/", replacement);
			}

			if(result.contains("${locale}")){
				result = result.replace("${locale}", locale.toLowerCase());
			}
		}
		return result;
	}
}
