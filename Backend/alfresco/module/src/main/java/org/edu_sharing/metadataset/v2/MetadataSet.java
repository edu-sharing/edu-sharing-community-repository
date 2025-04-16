package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionBaseService;
import org.edu_sharing.metadataset.v2.MetadataWidget.Subwidget;
import org.edu_sharing.repository.client.tools.CCConstants;

@Data
public class MetadataSet implements Serializable {
	static Logger logger = Logger.getLogger(MetadataSet.class);

	public static String DEFAULT_CLIENT_QUERY="ngsearch";
	public static String DEFAULT_CLIENT_QUERY_CRITERIA = "ngsearchword";	
	
	private String id,repositoryId,label,i18n,name,inherit;
	private List<MetadataWidget> widgets;
	private List<AiConfig> aiConfigs;
	private boolean hidden;
	private List<MetadataTemplate> templates;
	private List<MetadataGroup> groups;
	private List<MetadataList> lists;
	private List<MetadataSort> sorts;
	private Map<String, MetadataQueries> queries;
	private MetadataCreate create;

	public MetadataQueries getQueries(String syntax) {
		return queries.get(syntax);
	}

	public void overrideWith(MetadataSet mdsOverride) {
		if(mdsOverride.getId()!=null)
			setId(mdsOverride.getId());
		if(mdsOverride.getName()!=null)
			setName(mdsOverride.getName());
		for(MetadataWidget widget : mdsOverride.getWidgets()){
			if(!widget.isInherit()){
				List<MetadataWidget> widgetsRemove = findAllWidgets(widget.getId());
				widgets.removeAll(widgetsRemove);
			}
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
		for(MetadataSort sort : mdsOverride.getSorts()){
			if(sorts.contains(sort)){
				sorts.remove(sort);
				sorts.add(0,sort);
			}
			else{
				sorts.add(0,sort);
			}
		}
		if(mdsOverride.getCreate()!=null) {
			setCreate(mdsOverride.getCreate());
		}
		for(Map.Entry<String, MetadataQueries> querySet : mdsOverride.getQueries().entrySet()){
			queries.getOrDefault(querySet.getKey(),new MetadataQueries())
					.overrideWith(mdsOverride.getQueries(querySet.getKey()));
		}
	}
	public MetadataWidget findWidget(String widgetId) {
		for(MetadataWidget widget : widgets){
			if(widget.getId().equals(widgetId))
				return widget;
		}
		throw new IllegalArgumentException("Widget "+widgetId+" was not found in the mds "+id);
	}
	public List<MetadataWidget> findAllWidgets(String widgetId) {
		List<MetadataWidget> found = new ArrayList<>();
		for(MetadataWidget widget : widgets){
			if(widget.getId().equals(widgetId))
				found.add(widget);
		}
		if(!found.isEmpty()) {
			return found;
		}
		throw new IllegalArgumentException("Widget "+widgetId+" was not found in the mds "+id);
	}
	public MetadataGroup findGroup(String groupId) {
		for(MetadataGroup group : groups){
			if(group.getId().equals(groupId))
				return group;
		}
		throw new IllegalArgumentException("Group "+groupId+" was not found in the mds "+id);
	}
	public MetadataTemplate findTemplate(String templateId) {
		for(MetadataTemplate template : templates){
			if(template.getId().equals(templateId))
				return template;
		}
		throw new IllegalArgumentException("Template "+templateId+" was not found in the mds "+id);
	}
	public MetadataQuery findQuery(String queryId, String syntax) {
		try {
			return queries.get(syntax).findQuery(queryId);
		}catch(IllegalArgumentException e){
			throw new IllegalArgumentException("Query id "+queryId+" not found using syntax " + syntax, e);
		}
	}
	public Collection<MetadataWidget> getWidgetsByNode(String nodeType,Collection<String> aspects, boolean onlyPrimaryWidgets) {
		List<String> group=null;
		if(CCConstants.CCM_TYPE_IO.equals(nodeType)) {
			group= Collections.singletonList("io");
		}
		else if(CCConstants.CCM_TYPE_MAP.equals(nodeType)) {
			if(aspects.contains(CCConstants.CCM_ASPECT_COLLECTION)){
				group=Arrays.asList("collection_editorial", "io");
			}
			else {
				group = Collections.singletonList("map");
			}
		}
		if(group==null) {
			logger.info("Node type "+nodeType+" currently not supported by backend, will use metadata from all available widgets");
			return getWidgets();
		}
		if(onlyPrimaryWidgets) {
			return getWidgetsByTemplate(group.get(0));
		} else {
			HashSet<MetadataWidget> result = new HashSet<MetadataWidget>();
			for(String g: group) {
				result.addAll(getWidgetsByTemplate(g));
			}
			return result;
		}
	}

	public List<MetadataWidget> getWidgetsByTemplate(String template) {
		List<MetadataWidget> usedWidgets=new ArrayList<>();
		for(String view : findGroup(template).getViews()) {
			String html = findTemplate(view).getHtml();
			for(MetadataWidget widget : getWidgets()) {
				if(html.contains("<" + widget.getId())) {
					usedWidgets.add(widget);
					// handle group (sub) widgets
					if(widget.getSubwidgets()!=null && !widget.getSubwidgets().isEmpty()) {
						for(Subwidget subwidget : widget.getSubwidgets()) {
							usedWidgets.addAll(findAllWidgets(subwidget.getId()));
						}
					}
				}
			}
		}
		return usedWidgets;
	}

	public MetadataWidget findWidgetForTemplateAndCondition(String widgetId, String template, Map<String, String[]> properties) {
		  List<MetadataWidget> found=new ArrayList<>();
		  boolean hasTemplate=false;
		  for(MetadataWidget widget : widgets){
			  if(widget.getId().equals(widgetId) && widget.getTemplate()==null)
					found.add(widget);
			  if(widget.getId().equals(widgetId) && template.equals(widget.getTemplate())) {
					if(!hasTemplate) found.clear();
					hasTemplate=true;
					found.add(widget);
			  }
		  }
		  if(found.isEmpty()) {
			  throw new IllegalArgumentException("Widget " + widgetId + " was not found in the mds " + id);
		  }
		  List<MetadataWidget> result=new ArrayList<>();
		  for(MetadataWidget widget : found) {
			  boolean allowed = true;
			  MetadataCondition cond = widget.getCondition();
			  if (cond != null){
				  if(cond.getType().equals(MetadataCondition.CONDITION_TYPE.PROPERTY)) {
				  	  // properties are already local names
					  String[] value=properties.get(cond.getValue());
					  if(cond.getPattern() != null){
					  	// regex pattern check
					  	if(value!=null && value.length > 0 && value[0] != null) {
							Pattern pattern = Pattern.compile(cond.getPattern());
							Matcher matcher = pattern.matcher(value[0]);
							allowed = matcher.matches() != cond.isNegate();
						} else {
					  		// no value, so fallback to "false"
					  		allowed = cond.isNegate();
						}
					  } else {
					  	// primitive isEmpty check
						  boolean empty = isValueEmpty(value);
						  allowed = empty == cond.isNegate();
					  }
				  } else if (cond.getType().equals(MetadataCondition.CONDITION_TYPE.TOOLPERMISSION)) {
					  boolean hasTp = new ToolPermissionBaseService().hasToolPermission(cond.getValue());
					  allowed=hasTp!=cond.isNegate();
				  }
			  }
			  if(allowed) {
				  result.add(widget);
			  }
		  }
		// no condition matched
		if(result.isEmpty()) {
			return null;
		}
        if (result.size() > 1) {
			logger.warn("Widget " + widgetId + " has multiple candidates (" + result.size() + ") when rendered with template " + template + ", will use the first one that matches. Check the metadataset definitions for that widget to ensure only one candidate always matches.");
		}
		result.get(0).setHideIfEmpty(true);
		return result.get(0);
	 }

	private boolean isValueEmpty(String[] value) {
		boolean empty=value==null || value.length==0;
		if(!empty){
			empty=true;
			for(String check :value){
                if (check != null && !check.isEmpty()) {
                    empty = false;
                    break;
                }
		}
	  }
		return empty;
	}
}
