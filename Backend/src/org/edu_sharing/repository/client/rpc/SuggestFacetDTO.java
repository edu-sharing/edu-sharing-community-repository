package org.edu_sharing.repository.client.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.SuggestOracle;

public class SuggestFacetDTO implements IsSerializable, SuggestOracle.Suggestion,  HasKey {

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

	@Override
	public String getDisplayString() {
		if(displayString!=null)
			return displayString;
		return facet;
	}

	public void setDisplayString(String displayString) {
		this.displayString = displayString;
	}

	@Override
	public String getReplacementString() {
		return facet;
	}

	
}
