package org.edu_sharing.repository.client.rpc;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.SuggestOracle;

public class Suggestion implements SuggestOracle.Suggestion, IsSerializable, HasKey{
	
	MetadataSetValueKatalog cat;
	
	String locale;
	public Suggestion() {
	}
	
	public Suggestion(String locale) {
		this.locale = locale;
	}
	
	@Override
	public String getDisplayString() {
		return this.cat.getValue(this.locale);
	}
	
	@Override
	public String getReplacementString() {
		return this.cat.getValue(this.locale);
	}

	public void setCat(MetadataSetValueKatalog cat) {
		this.cat = cat;
	}
	
	@Override
	public String getKey() {
		return cat.getKey();
	}
	
}
