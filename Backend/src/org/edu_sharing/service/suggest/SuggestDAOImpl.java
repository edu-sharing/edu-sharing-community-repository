package org.edu_sharing.service.suggest;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.Suggestion;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.Context;

import com.google.gwt.user.client.ui.SuggestOracle;

public class SuggestDAOImpl implements SuggestDAO {

	Logger logger = Logger.getLogger(SuggestDAOImpl.class);
		
	MetadataSetBaseProperty property;
	
	@Override
	public List<? extends  SuggestOracle.Suggestion> query(String query) {
		
		List<SuggestOracle.Suggestion> result = new ArrayList<SuggestOracle.Suggestion>();
		
		List<MetadataSetValueKatalog> cata = property.getValuespace();
		
		String locale = (String)Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.AUTH_LOCALE);
		
		
		if(locale == null || locale.trim().equals("")) locale = "en_EN";
		
		
		
		for(MetadataSetValueKatalog cat:cata){
			String val = cat.getValue(locale);
			
			if(query.equals("-all-")){
				Suggestion sugg = new Suggestion(locale);
				sugg.setCat(cat);
				result.add(sugg);
			}else{
				if(val.toLowerCase().contains(query.toLowerCase())){
					Suggestion sugg = new Suggestion(locale);
					sugg.setCat(cat);
					result.add(sugg);
				}
			}
	
		}
		
		return result;
		
	}
	
	@Override
	public String getValue(String key) {
		List<MetadataSetValueKatalog> cata = property.getValuespace();
		
		String locale = (String)Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.AUTH_LOCALE);
		if(locale == null || locale.trim().equals("")) locale = "en_EN";
		
		for(MetadataSetValueKatalog cat:cata){
			if(cat.getKey().equals(key)){
				return  cat.getValue(locale);
			}
		}
		
		//return request key when it's a free value config
		if(property.getWidget().equals(MetadataSetBaseProperty.WIDGET_SUGGESTBOX_BIGDATA_FREE)){
			return key;
		}else{
			return null;
		}
	}
	
	@Override
	public void setMetadataProperty(MetadataSetBaseProperty property) {
		this.property = property;
		
	}
	
	
}
