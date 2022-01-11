package org.edu_sharing.metadataset.v2;


public class MetadataTemplate extends MetadataTranslatable {

	public static enum REL_TYPE{
		suggestions
	};

	private REL_TYPE rel;
	private String id,caption,icon,html;
	private boolean hideIfEmpty=false;
	private boolean extended;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCaption() {
		//return MetadataReaderV2.getTranslation(this, caption, locale);
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}
	
	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
	
	public REL_TYPE getRel() {
		return rel;
	}

	public void setRel(REL_TYPE rel) {
		this.rel = rel;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataTemplate){
			return ((MetadataTemplate)obj).id.equals(id);
		}
		return super.equals(obj);
	}

    public void setHideIfEmpty(boolean hideIfEmpty) {
        this.hideIfEmpty = hideIfEmpty;
    }

    public boolean getHideIfEmpty() {
        return hideIfEmpty;
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public boolean isExtended() {
        return extended;
    }
}
