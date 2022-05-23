package org.edu_sharing.repository.client.rpc;


import java.io.Serializable;

public class SuggestFacetDTO implements Serializable, HasKey {

	String facet;
	String displayString;
		
	public SuggestFacetDTO(){
		
	}

	public String getFacet() {
		return facet;
	}

	public void setFacet(String facet) {
		this.facet = facet;
	}

	@Override
	public String getKey() {
		return facet;
	}

	public String getDisplayString() {
		if(displayString!=null)
			return displayString;
		return facet;
	}

	public void setDisplayString(String displayString) {
		this.displayString = displayString;
	}

	public String getReplacementString() {
		return facet;
	}

	
}
