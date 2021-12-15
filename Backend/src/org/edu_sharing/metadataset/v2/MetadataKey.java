package org.edu_sharing.metadataset.v2;

import java.util.ArrayList;
import java.util.List;

public class MetadataKey extends MetadataTranslatable {

	public static class MetadataKeyRelated extends MetadataKey {
		public enum Relation {
			exactMatch,
			narrowMatch,
			relatedMatch,
			closeMatch,
			broadMatch,
		};
		private Relation relation;

		public MetadataKeyRelated(Relation relation) {
			this.relation = relation;
		}

		public Relation getRelation() {
			return relation;
		}
	}
	private String key,caption,description,parent,locale;
	/**
	 * List of other keys this child is a precedor of
	 */
	private List<String> preceds;

	private final List<MetadataKeyRelated> related = new ArrayList<>();

	private List<String> alternativeKeys;

	private String url;

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setAlternativeKeys(List<String> alternativeKeys) {
		this.alternativeKeys = alternativeKeys;
	}

	public List<String> getAlternativeKeys() {
		return alternativeKeys;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		if ( parent==null || parent.trim().isEmpty() )
			this.parent=null;
		else
			this.parent = parent;
	}

	public void addRelated(MetadataKeyRelated related) {
		this.related.add(related);
	}

	public List<MetadataKeyRelated> getRelated() {
		return related;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public List<String> getPreceds() {
		return preceds;
	}

	public void setPreceds(List<String> preceds) {
		this.preceds = preceds;
	}

	public void setLocale(String locale) { this.locale = locale;}

	public String getLocale() { return locale;}
}
