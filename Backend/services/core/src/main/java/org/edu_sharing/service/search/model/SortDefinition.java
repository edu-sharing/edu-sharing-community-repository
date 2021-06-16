package org.edu_sharing.service.search.model;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class SortDefinition implements Serializable {

	transient ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	transient ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	List<SortDefinitionEntry> sortDefinitionEntries = new ArrayList<SortDefinitionEntry>();

    public static class SortDefinitionEntry implements Serializable{
		String property;
		boolean ascending;
		
		public String getProperty() {
			return property;
		}
		
		public void setProperty(String property) {
			this.property = property;
		}
		
		public boolean isAscending() {
			return ascending;
		}
		
		public void setAscending(boolean ascending) {
			this.ascending = ascending;
		}
		public SortDefinitionEntry(){}
		public SortDefinitionEntry(String property,boolean ascending){
			this.property=property;
			this.ascending=ascending;
		}
	}
	public SortDefinition(){}
	/**
	 * Fills this SortDefinition with a string list of properties
	 * @param sortProperties the names to sort by
	 * @param sortAscending sort ascending or descending
	 */
	public SortDefinition(Iterable<String> sortProperties,List<Boolean> sortAscending){
		this(null,sortProperties,sortAscending);		
	}
	public SortDefinition(String namespace, Iterable<String> sortProperties, List<Boolean> sortAscending) {
		if(sortProperties == null)
			return;			
		if(sortAscending==null) {
			sortAscending=new ArrayList<Boolean>();
			sortAscending.add(new Boolean(true));
		}
		int i=0;
		for(String sortProp : sortProperties){
				SortDefinitionEntry entry = new SortDefinitionEntry();
				Boolean sortAsc=sortAscending.size()==1 ? sortAscending.get(0) : sortAscending.get(i);
				entry.setAscending(sortAsc);
				entry.setProperty(namespace!=null && sortProp.split(":").length == 1 ? "{"+namespace+"}"+sortProp : sortProp);
				addSortDefinitionEntry(entry);
				i++;
			}
	}
	public List<SortDefinitionEntry> getSortDefinitionEntries() {
		return sortDefinitionEntries;
	}
	public void addSortDefinitionEntry(SortDefinitionEntry sortDefinitionEntry,int position){
		sortDefinitionEntries.add(position,sortDefinitionEntry);
	}
	public void addSortDefinitionEntry(SortDefinitionEntry sortDefinitionEntry){
		sortDefinitionEntries.add(sortDefinitionEntry);
	}
	/** 
	 * 
	 * @return true if this SortDefinition has entries
	 */
	public boolean hasContent() {
		return !sortDefinitionEntries.isEmpty();
	}
	public List<Pair<QName, Boolean>> asSortProperties() {
		if(!hasContent())
			return null;
		List<Pair<QName, Boolean>> list=new ArrayList<Pair<QName, Boolean>>();
		for(SortDefinitionEntry entry : sortDefinitionEntries){
			list.add(new Pair<QName, Boolean>(QName.createQName(entry.property),entry.ascending));
		}
		return list;
	}
	public String getFirstSortBy() {
		if(!hasContent())
			return null;
		return sortDefinitionEntries.get(0).property;
	}
	public boolean getFirstSortAscending() {
		if(!hasContent())
			return true;
		return sortDefinitionEntries.get(0).ascending;
	}
	public void applyToSearchParameters(SearchParameters searchParameters) {
		// Group by Folders & Files
		sortDefinitionEntries.add(0, new SortDefinitionEntry("TYPE",false));
		for (SortDefinitionEntry sortDefintionEntry : getSortDefinitionEntries()) {
			searchParameters.addSort(sortDefintionEntry.getProperty(), sortDefintionEntry.isAscending());
		}
	}

	public void applyToSearchSourceBuilder(SearchSourceBuilder searchSourceBuilder) {
		// Group by Folders & Files
		searchSourceBuilder.sort("type", SortOrder.ASC);
		for (SortDefinitionEntry sortDefintionEntry : getSortDefinitionEntries()) {
			SortOrder sortOrder = sortDefintionEntry.ascending ? SortOrder.ASC : SortOrder.DESC;
			if(sortDefintionEntry.getProperty().equalsIgnoreCase("score")) {
				searchSourceBuilder.sort(new ScoreSortBuilder().order(sortOrder));
			} else {

				boolean addKeywordSuffix = false;
				String property = CCConstants.getValidGlobalName(sortDefintionEntry.getProperty());
				if(property != null){
					PropertyDefinition propDef = serviceRegistry.getDictionaryService().getProperty(QName.createQName(property));
					if(propDef != null) {
						if (DataTypeDefinition.TEXT.equals(propDef.getDataType().getName())
								|| DataTypeDefinition.MLTEXT.equals(propDef.getDataType().getName())) {
							addKeywordSuffix = true;
						}
					}
				}
				String name = "properties." + sortDefintionEntry.getProperty() + ((addKeywordSuffix) ? ".keyword" :"" );
				searchSourceBuilder.sort(name, sortOrder);
			}
		}
	}
}
