package org.edu_sharing.service.license;

import org.apache.axis.utils.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.tools.URLHelper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicenseService {
	private static String DEFAULT_LICENSE_VERSION = "4.0";

	/**
	 * Result of {@link #parseLicenseUrl(String)}.
	 * licenseKey is one of the {@link CCConstants}.COMMON_LICENSE_* keys,
	 * language is the deed language (may be null if the url contains no deed suffix),
	 * version is the license version (e.g. "4.0").
	 */
	public record LicenseUrl(String licenseKey, String language, String version) {}

	private static final Map<String, String> CC_LICENSE_CODES = Map.of(
			"by", CCConstants.COMMON_LICENSE_CC_BY,
			"by-sa", CCConstants.COMMON_LICENSE_CC_BY_SA,
			"by-nd", CCConstants.COMMON_LICENSE_CC_BY_ND,
			"by-nc", CCConstants.COMMON_LICENSE_CC_BY_NC,
			"by-nc-sa", CCConstants.COMMON_LICENSE_CC_BY_NC_SA,
			"by-nc-nd", CCConstants.COMMON_LICENSE_CC_BY_NC_ND
	);
	// e.g. https://creativecommons.org/licenses/by-sa/3.0/de/deed.en (locale and deed are optional)
	private static final Pattern CC_LICENSE_PATTERN = Pattern.compile(
			"^https?://(?:www\\.)?creativecommons\\.org/licenses/([a-z-]+)/(\\d+\\.\\d+)(?:/[a-z]{2}(?:_[A-Za-z]{2})?)?(?:/deed\\.([a-zA-Z]{2}(?:_[A-Z]{2})?))?/?$");
	// e.g. https://creativecommons.org/publicdomain/zero/1.0/deed.de (deed is optional)
	private static final Pattern CC_PUBLIC_DOMAIN_PATTERN = Pattern.compile(
			"^https?://(?:www\\.)?creativecommons\\.org/publicdomain/(zero|mark)/(\\d+\\.\\d+)(?:/deed\\.([a-zA-Z]{2}(?:_[A-Z]{2})?))?/?$");
	// e.g. http://edu-sharing.net/licenses/edu-nc-nd/1.0/de
	private static final Pattern EDU_LICENSE_PATTERN = Pattern.compile(
			"^https?://(?:www\\.)?edu-sharing\\.net/licenses/(edu-nc-nd|custom-licence)/(\\d+\\.\\d+)/([a-z]{2})/?$");

	/**
	 * Parses a license url (the inverse of {@link #getLicenseUrl(String, String, String, String)}).
	 * @throws IllegalArgumentException if the given url is not a valid license url
	 */
	public LicenseUrl parseLicenseUrl(String url) {
		if (StringUtils.isEmpty(url)) {
			throw new IllegalArgumentException("No license url given");
		}
		String trimmed = url.trim();
		Matcher matcher = CC_LICENSE_PATTERN.matcher(trimmed);
		if (matcher.matches()) {
			String licenseKey = CC_LICENSE_CODES.get(matcher.group(1));
			if (licenseKey == null) {
				throw new IllegalArgumentException("Unknown creative commons license code in url: " + url);
			}
			return new LicenseUrl(licenseKey, matcher.group(3), matcher.group(2));
		}
		matcher = CC_PUBLIC_DOMAIN_PATTERN.matcher(trimmed);
		if (matcher.matches()) {
			String licenseKey = "zero".equals(matcher.group(1))
					? CCConstants.COMMON_LICENSE_CC_ZERO
					: CCConstants.COMMON_LICENSE_PDM;
			return new LicenseUrl(licenseKey, matcher.group(3), matcher.group(2));
		}
		matcher = EDU_LICENSE_PATTERN.matcher(trimmed);
		if (matcher.matches()) {
			String licenseKey = "edu-nc-nd".equals(matcher.group(1))
					? CCConstants.COMMON_LICENSE_EDU_NC_ND
					: CCConstants.COMMON_LICENSE_CUSTOM;
			return new LicenseUrl(licenseKey, matcher.group(3), matcher.group(2));
		}
		throw new IllegalArgumentException("Not a valid license url: " + url);
	}

	public String getIconUrl(String license,boolean dynamic){
		if(license==null || license.isEmpty())
			license="none";
		String ccImageName = license.toLowerCase().replace("_", "-")+".svg";
		String url = URLHelper.getBaseUrl(dynamic) + "/ccimages/licenses/" + ccImageName;

		return url;
	}
	public String getIconUrl(String license){
		return getIconUrl(license,true);
	}

	public String getLicenseUrl(String license, String locale){
		return getLicenseUrl(license, locale, null);
	}
	public String getLicenseUrl(String license, String locale, String version){
		return getLicenseUrl(license, locale, version, AuthenticationToolAPI.getInstance().getCurrentLanguage());
	}

	public String getLicenseUrl(String license, String locale, String version, String userLanguage){
		if(license==null || license.isEmpty())
			return null;
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
			version = (version == null) ? DEFAULT_LICENSE_VERSION : version;
			if(result.contains("{{version}}")){
				result = result.replace("{{version}}", version);
			}
			
			String country = (locale == null ? "de" : locale.split("_")[0]).toLowerCase() + "/";
			if(StringUtils.isEmpty(locale) || "4.0".equals(version)) {
				country = "";
			}
			if(result.contains("{{locale}}")){
				result = result.replace("{{locale}}", country);
			}

			if(result.contains("{{language}}")){
				result = result.replace("{{language}}", (StringUtils.isEmpty(userLanguage) ? "en" : userLanguage));
			}
		}
		
		return result;
	}

}
