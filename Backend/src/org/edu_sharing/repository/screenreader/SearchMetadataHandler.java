/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.screenreader;

import java.util.HashMap;
import java.util.Vector;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValue;
import org.edu_sharing.repository.server.RepoFactory;


/**
 * Takes care about the metadata serach data and fields
 * @author Christian
 *
 */
public class SearchMetadataHandler {
	
	// holds the constructed HTML represenation of the metadata
	private String medatadataHtmlForm 			= "";
	
	// remembers which metadata sets were already added
	private Vector<String> addedMetadatasets	= new Vector<String>();
	
	// keep track of running numbers in HTML
	private int inputCounter 	= 1;
	private int tabCounter 		= 3;
	private int idCounter 		= 0;
	
	// the local/language the metadata is presented in
	private String locale;
	
	// the HTTP request 
	private javax.servlet.ServletRequest req;
	
	private String repositoryId;
	private String metadataSetId;
		
	public SearchMetadataHandler(String repositoryId) {
		this.repositoryId = repositoryId;
		this.metadataSetId = Const.METADATASET_SCREENREADER;
	}	
	
	/*
	 * a adoption of: SearchGuiHandler - addMetadataSearchCriterias
	 */
	private void addMetadataSearchCriterias(String repositoryId, String metadataSetId) {
	
		// prevent double adding of a metadata set
		String metadataSetCompleteID = repositoryId+"/"+metadataSetId;
		if (this.addedMetadatasets.contains(metadataSetCompleteID)) {
			return;
		} else {
			this.addedMetadatasets.add(metadataSetCompleteID);
		}
		
		// add info to html form
		this.medatadataHtmlForm += "<input type='hidden' name='"+Const.PARA_REPO+"' value='"+repositoryId+"'>\n";	
		
		MetadataSet mds = null;
		try {
			mds = RepoFactory.getStandaloneMetadataSet(Const.METADATASET_SCREENREADER_PATH);		
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println("SETUP ERROR - Metadataset for Screenreader not able to set ("+Const.METADATASET_SCREENREADER_PATH+")");			
			return;
		}
		
				
		// cycle thru all metadata queries
		for (MetadataSetQuery mdsQuery : mds.getMetadataSetQueries().getMetadataSetQueries()) {		
			
			String label 		= null;
			String key			= null;
			String stylename 	= null;			
			
			// get lable auf query (in local language)
			MetadataSetValue mdsvLbl = mdsQuery.getLabel();
			if (mdsvLbl != null) {
				label = mdsvLbl.getValue(locale);
				key = mdsvLbl.getKey();
			}

			// if no extra lable provided take the label of the first meta data property			
			if (label == null) {
				if (mdsQuery.getProperties() != null && mdsQuery.getProperties().size() > 0) {
					MetadataSetQueryProperty prop = mdsQuery.getProperties().get(0);
					if (prop.getLabel() != null) {
						label = prop.getLabel().getValue(locale);
						key = prop.getLabel().getKey();
					}
				}
				label = (label == null) ? "" : label;
			}		
			
			// get stylename
			if (mdsQuery.getStylename() != null && !mdsQuery.getStylename().equals("")) {
				stylename = mdsQuery.getStylename();
			}
			stylename = (stylename == null) ? "" : stylename;	
			key = (key == null) ? "" : key;	
						
			// reduce search complexity by skipping certain fields
			if (key.equals("pwheader_learningTimePW")) continue;
			if (key.equals("lom_prop_educational_learningresourcetype")) continue;
			if (key.equals("pwheader_learningGroupSize")) continue;
			if (key.equals("pwheader_timerange")) continue;
			if (key.equals("pwheader_user")) continue;
			
			// cycle thru the properties of metadata query
			try {
				String queryHTML = "";
				for (MetadataSetQueryProperty prop : mdsQuery.getProperties()) {	
					String propHtml =  FormsUiBuilder.getFormInputHTML(prop, this, key);
					queryHTML += "\n"+propHtml;			
				}		
				this.medatadataHtmlForm += FormsUiBuilder.wrappIntoFieldset(label, stylename, queryHTML, this);
			} catch (MetadataMailformedException mme) {				
				this.medatadataHtmlForm += "\n<!-- "+mme.getMessage()+"  -->\n";
			}
		}
	}
	
	/**
	 * Returns the HTML represenation of all Inputs  
	 * @return
	 */
	public String getHtmlFormInputs(String locale, int lastTabIndex, int lastFieldIndex, javax.servlet.ServletRequest req) {
		this.req = req;
		this.locale = locale;		
		this.inputCounter = lastFieldIndex;
		this.tabCounter = lastTabIndex;
		if (this.medatadataHtmlForm.length()==0) this.addMetadataSearchCriterias(this.repositoryId,this.metadataSetId);		
		if (this.medatadataHtmlForm.length()==0) return "\n<!-- NO SEARCH METADATA ADDED -->\n";
		return this.medatadataHtmlForm; 	
	}
	
	/*
	 * a adoption of: SearchGuiHandlerBase - getData
	 * Builds search criteria from HTTP request data 
	 * complete data model is in request parameters 
	 */
	public HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> getSearchData(javax.servlet.ServletRequest req) throws MetadataMailformedException{
		
		this.req = req;
		
		// prepare result data
		HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> result = new HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>>();

		MetadataSet mds = null;
		
		try {
			mds = RepoFactory.getStandaloneMetadataSet(Const.METADATASET_SCREENREADER_PATH);		
		} catch (Throwable e) {
			System.err.println("ERROR on org.edu_sharing.repository.screenreader.SearchMetadataHandler: "+e.getMessage());
			throw new MetadataMailformedException("Serverfehler");		
		}
		
		// cycle thru all metadata queries
		for (MetadataSetQuery mdsQuery : mds.getMetadataSetQueries().getMetadataSetQueries()) {		
			
			HashMap<MetadataSetQueryProperty, String[]> propValList = new HashMap<MetadataSetQueryProperty, String[]>();			
			
			// get section key
			String key			= null;		
			MetadataSetValue mdsvLbl = mdsQuery.getLabel();
			if (mdsvLbl != null) {
				key = mdsvLbl.getKey();
			}		
			if (key == null) {
				if (mdsQuery.getProperties() != null && mdsQuery.getProperties().size() > 0) {
					MetadataSetQueryProperty prop = mdsQuery.getProperties().get(0);
					if (prop.getLabel() != null) {
						key = prop.getLabel().getKey();
					}
				}
			}	
			key = (key == null) ? "" : key;	
			
			try {
			
				for (MetadataSetQueryProperty prop : mdsQuery.getProperties()) {
					String[] values = FormsUiReader.getFormValueFromParameter(prop, this, key);
					if ((values!=null) && (values.length>0)) {
						propValList.put(prop, values);
					}
				}
				
				if (propValList.size() > 0) {
					result.put(mdsQuery, propValList);
				}
				
			} catch (MetadataQueryNotSupportedException e) { 
				// ignore not supported metadata
			}

		}
		
		return result;
	}
	
	public int getNextTabIndex() {
		this.tabCounter++;
		return this.tabCounter;
	}
	
	public int getNextFormFieldIndex() {
		this.inputCounter++;
		return this.inputCounter;
	}
	
	public String getNextUniqueElementId() {
		this.idCounter++;
		return "element"+this.idCounter;
	}
	
	public String getLanguage() {
		return this.locale;
	}
	
	/**
	 * Checks if a value is set as parameter to the HTTP request
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getParameterFromRequest(String key, String defaultValue) {
		return JspTools.getParameterOrDefault(this.req, key, defaultValue);
	}
	
	/**
	 * Checks if multiple values as pararmeter to the HTTP request
	 * @param key
	 * @return
	 */
	public String[] getParametersFromRequest(String key) {
		return this.req.getParameterValues(key);
	}
	
	/**
	 * Checks if a value was within a multiple selection as pararmeter to the HTTP request
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean isValueInParameters(String key, String value) {
		String[] values = getParametersFromRequest(key);
		if (values!=null) {
			for (int i=0; i<values.length; i++) {
				if (values[i].equals(value)) return true;
			}
		}
		return false;
	}
}
