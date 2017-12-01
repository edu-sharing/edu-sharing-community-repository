package org.edu_sharing.service.suggest;

import java.util.List;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;

import com.google.gwt.user.client.ui.SuggestOracle;

public interface SuggestDAO {

	List<? extends  SuggestOracle.Suggestion> query(String query);
	
	public void setMetadataProperty(MetadataSetBaseProperty property);
	
	public String getValue(String key);
}
