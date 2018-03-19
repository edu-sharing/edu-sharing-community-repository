package org.edu_sharing.metadataset.v2;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.service.license.LicenseService;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar.FormatConfiguration;

import jersey.repackaged.com.google.common.collect.Lists;

import java.lang.IllegalArgumentException;



/**
 * Class for rendering mds templates for the RenderService as html
 * Needs the active mds and the node properties
 * @author Torsten
 *
 */
public class MetadataTemplateRenderer {

	private static final String GROUP_MULTIVALUE_DELIMITER = "[+]";
	private MetadataSetV2 mds;
	private Map<String, String[]> properties;

	public MetadataTemplateRenderer(MetadataSetV2 mds,Map<String,String[]> properties) {
		this.mds = mds;
		this.properties = properties;
	}

	public String render(String groupName) throws IllegalArgumentException {
		for(MetadataGroup group : mds.getGroups()){
			if(group.getId().equals(groupName))
				return render(group);
		}
		return null;
	}

	private String render(MetadataGroup group) throws IllegalArgumentException {
		String html="";
		for(String view : group.getViews()){
			boolean found=false;
			for(MetadataTemplate template : mds.getTemplates()){
				if(template.getId().equals(view)){
					html += renderTemplate(template);
					found=true;
					break;
				}
			}
			if(!found) {
				html += "Error: View "+view+" was included in group "+group.getId()+" but not found in template list";
			}
		}
		return html;
	}

	private String renderTemplate(MetadataTemplate template) throws IllegalArgumentException {
		String html="";
		html+="<div class='mdsGroup'><div class='mdsCaption "+template.getId()+"'>"+template.getCaption()+"</div>"
				+ "<div class='mdsContent'>";
		String content=template.getHtml();
		
		for(MetadataWidget widget : mds.getWidgets()){
			int start=content.indexOf("<"+widget.getId());
			if(start==-1)
				continue;
			int end=content.indexOf(">",start);
			String first=content.substring(0, start);
			String second=content.substring(end+1);
			String attributes=content.substring(start+1+widget.getId().length(),end);
			widget=applyAttributes(widget,attributes);
			String[] values=properties.get(widget.getId());
			boolean wasEmpty=false;
			if(values==null){
				values=new String[]{"-"};
				wasEmpty=true;
			}
			String widgetHtml="<div class='mdsWidget'"+attributes+"><div class='mdsWidgetCaption'>"+widget.getCaption()+"</div>";
			widgetHtml+="<div class='mdsWidgetContent mds_"+widget.getId().replace(":","_");
			if(widget.isMultivalue())
				widgetHtml+=" mdsWidgetMultivalue";
			widgetHtml+="'>";
			Map<String, MetadataKey> valuesMap = widget.getValuesAsMap();
			boolean empty=true;
			if("multivalueTree".equals(widget.getType())) {
				Map<Integer,List<String>> map=new HashMap<>();
				for(String value:values) {
					MetadataKey key=valuesMap.get(value);
					if(key==null)
						continue;
					List<String> path=new ArrayList<String>();
					while(key!=null) {
						path.add(key.getCaption());
						key=valuesMap.get(key.getParent());
					}
					path=Lists.reverse(path);
					int i=0;
					widgetHtml+="<div>";
					empty=empty && path.size()==0;
					for(String p : path) {
						if(i>0) {
							widgetHtml+="<i class='material-icons'>keyboard_arrow_right</i>";
						}
						widgetHtml+=p;
						i++;
					}
					widgetHtml+="</div>";

				}
			}
			else {
				for(String value : values){
					if(widget.getId().equals("license")){
						String licenseName=properties.containsKey(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)) ?
								   properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY))[0] : null;
						String licenseVersion=properties.containsKey(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION)) ?
									properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION))[0] : null;


						LicenseService license=new LicenseService();
						String link=license.getLicenseUrl(licenseName,mds.getI18n(), licenseVersion);
						value="";
						if(link!=null)
							value="<a href='"+link+"' target='_blank'>";
						value+="<img src='"+
								license.getIconUrl(licenseName)+
								"'>";
						if(CCConstants.COMMON_LICENSE_CUSTOM.equals(licenseName) && properties.containsKey(CCConstants.getValidLocalName(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION))) {
							String licenseDescription=properties.get(CCConstants.getValidLocalName(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION))[0];
							value+="<div class='licenseDescription'>"+StringEscapeUtils.escapeHtml(licenseDescription)+"</div>";
						}
						if(link!=null)
							value+="</a>";
					}
					if(value==null || value.trim().isEmpty())
						continue;
					if(widget.getType()!=null){
						if(widget.getType().equals("date")){
							try{
								if(widget.getFormat()!=null && !widget.getFormat().isEmpty()){
									value=new SimpleDateFormat(widget.getFormat()).format(Long.parseLong(value));
								}
								else{
									value=new DateTool().formatDate(Long.parseLong(value));
								}
							}catch(Throwable t){
								// wrong data or text
							}
						}
						if(widget.getType().equals("filesize")){
							try{
								value=formatFileSize(Long.parseLong(value));
							}catch(Throwable t){
							}
						}
						if(widget.getType().equals("multivalueGroup")) {
                            value=formatGroupValue(value,widget);
                        }
                    }
					if(valuesMap.containsKey(value))
						value=valuesMap.get(value).getCaption();
					widgetHtml+="<div>";
					if(widget.getIcon()!=null){
						widgetHtml+=insertIcon(widget.getIcon());
					}
					widgetHtml+=value;
					widgetHtml+="</div>";
					if(!value.trim().isEmpty())
						empty=false;
				}
			}
			widgetHtml+="</div></div>";
			if((empty || wasEmpty) && widget.isHideIfEmpty())
				widgetHtml="";
			content=first+widgetHtml+second;
		}
		html+=content;
		html+="</div></div>";
		return html;
	}

	private String formatGroupValue(String value,MetadataWidget widget) {
		if(value==null)
			return null;
		String[] splitted = StringUtils.splitByWholeSeparatorPreserveAllTokens(value,MetadataTemplateRenderer.GROUP_MULTIVALUE_DELIMITER);
		String result="";
		int i=0;
		for(String s : splitted) {
			Map<String, MetadataKey> valuesMap = mds.findWidget(widget.getSubwidgets().get(i).getId()).getValuesAsMap();
			if(!s.isEmpty()) {
				if(!result.isEmpty())
					result+=", ";
				if(valuesMap.containsKey(s))
					s=valuesMap.get(s).getCaption();
				result+=s;
			}
			i++;
		}
		return result;
	}

	private MetadataWidget applyAttributes(MetadataWidget widget, String str) throws IllegalArgumentException {
		try {
			widget=widget.copyInstance();
		}catch(Throwable t) {
			throw new RuntimeException("Failed to serialize MetadataWidget class, check if all attributes are serializable",t);
		}
	    while(true){
	      str=str.substring(str.indexOf(" ")+1);
	      int pos=str.indexOf("=");
	      if(pos==-1) {
	        break;
	      }
	      String name=str.substring(0,pos).trim();
	      str=str.substring(pos+1);
	      String search=" ";
	      if(str.charAt(0)=='\''){
	        search="'";
	      }
	      if(str.charAt(0)=='"'){
	        search="\"";
	      }
	      if(search!=" ")
	        str=str.substring(1);
	      int end=str.indexOf(search);
	      String value=str.substring(0,end);
	      str=str.substring(end+1);
	      try{
	    		Field field = widget.getClass().getDeclaredField(name);
	    		field.setAccessible(true);
	    		field.set(widget, value);
	    	}catch(Throwable t){
	    		throw new IllegalArgumentException("Invalid attribute found for widget "+widget.getId()+", attribute "+name+" is unknown",t);
	    	}
	      }
	    return widget;
	}
	private String formatFileSize(long size) {
		String[] sizes=new String[]{"Bytes","KB","MB","GB","TB"};
		double outSize=size;
		int i=0;
		while(outSize>1500){
			outSize/=1024;
			i++;
		}
		NumberFormat format = NumberFormat.getInstance();
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(1);
		return format.format(outSize)+" "+sizes[i];
	}

	private String insertIcon(String name) {
		return "<i class='material-icons'>"+name+"</i>";
	}

}
