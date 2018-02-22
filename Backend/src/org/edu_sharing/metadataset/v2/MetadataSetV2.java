package org.edu_sharing.metadataset.v2;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;

public class MetadataSetV2 {
	
	public static String DEFAULT_CLIENT_QUERY="ngsearch";
	public static String DEFAULT_CLIENT_QUERY_CRITERIA = "ngsearchword";	
	
	private String id,repositoryId,label,i18n,name,inherit;
	private List<MetadataWidget> widgets;
	private boolean hidden;
	private List<MetadataTemplate> templates;
	private List<MetadataGroup> groups;
	private List<MetadataList> lists;
	private MetadataQueries queries;
	private MetadataCreate create;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}	
	
	public String getRepositoryId() {
		return repositoryId;
	}
	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getInherit() {
		return inherit;
	}
	public void setInherit(String inherit) {
		this.inherit = inherit;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public List<MetadataWidget> getWidgets() {
		return widgets;
	}
	public List<MetadataTemplate> getTemplates() {
		return templates;
	}
	public List<MetadataGroup> getGroups() {
		return groups;
	}
	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	public void setWidgets(List<MetadataWidget> widgets) {
		this.widgets = widgets;
	}
	public void setTemplates(List<MetadataTemplate> templates) {
		this.templates = templates;		
	}
	public void setGroups(List<MetadataGroup> groups) {
		this.groups = groups;		
	}
	public String getI18n() {
		return i18n;
	}
	public void setI18n(String i18n) {
		this.i18n = i18n;
	}
	public MetadataQueries getQueries() {
		return queries;
	}
	public void setQueries(MetadataQueries queries) {
		this.queries = queries;
	}
	public MetadataCreate getCreate() {
		return create;
	}
	public void setCreate(MetadataCreate create) {
		this.create = create;
	}
	public List<MetadataList> getLists() {
		return lists;
	}
	public void setLists(List<MetadataList> lists) {
		this.lists = lists;
	}
	public void overrideWith(MetadataSetV2 mdsOverride) {
		if(mdsOverride.getId()!=null)
			setId(mdsOverride.getId());
		if(mdsOverride.getName()!=null)
			setName(mdsOverride.getName());
		for(MetadataWidget widget : mdsOverride.getWidgets()){
			if(widgets.contains(widget)){
				widgets.remove(widget);
				widgets.add(0,widget);
			}
			else{
				widgets.add(0,widget);
			}
		}
		for(MetadataTemplate template : mdsOverride.getTemplates()){
			if(templates.contains(template)){
				templates.remove(template);
				templates.add(0,template);
			}
			else{
				templates.add(0,template);
			}
		}
		for(MetadataGroup group : mdsOverride.getGroups()){
			if(groups.contains(group)){
				groups.remove(group);
				groups.add(0,group);
			}
			else{
				groups.add(0,group);
			}
		}
		for(MetadataList list : mdsOverride.getLists()){
			if(lists.contains(list)){
				lists.remove(list);
				lists.add(0,list);
			}
			else{
				lists.add(0,list);
			}
		}
		if(mdsOverride.getCreate()!=null) {
			setCreate(mdsOverride.getCreate());
		}
		queries.overrideWith(mdsOverride.getQueries());
	}
	public MetadataWidget findWidget(String widgetId) {
		for(MetadataWidget widget : widgets){
			if(widget.getId().equals(widgetId))
				return widget;
		}
		throw new IllegalArgumentException("Widget "+widgetId+" was not found in the mds "+id);
	}
	public MetadataQuery findQuery(String queryId) {
		for(MetadataQuery query : queries.getQueries()){
			if(query.getId().equals(queryId))
				return query;
		}
		throw new IllegalArgumentException("Query "+queryId+" was not found in the mds "+id);
	}
	
}
