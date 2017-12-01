package org.edu_sharing.metadataset.v2;

public abstract class MetadataTranslatable {
	private String i18n;
	private String i18nPrefix;
	private String i18nFallback;
	public void setI18n(String i18n) {
		this.i18n=i18n;
	}
	public String getI18nPrefix() {
		return i18nPrefix;
	}
	public void setI18nPrefix(String i18nPrefix) {
		this.i18nPrefix = i18nPrefix;
	}
	public String getI18n() {
		return i18n;
	}
	public String getI18nFallback() {
		return i18nFallback;
	}
	public void setI18nFallback(String i18nFallback) {
		this.i18nFallback = i18nFallback;
	}
	
}
