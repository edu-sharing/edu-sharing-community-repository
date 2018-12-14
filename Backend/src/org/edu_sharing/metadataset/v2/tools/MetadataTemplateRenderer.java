package org.edu_sharing.metadataset.v2.tools;

import com.ibm.icu.text.SimpleDateFormat;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.license.LicenseService;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
	private static Logger logger=Logger.getLogger(MetadataTemplateRenderer.class);

	public MetadataTemplateRenderer(MetadataSetV2 mds,Map<String,String[]> properties) {
		this.mds = mds;
		this.properties = properties;
	}

	public String render(String groupName) throws IllegalArgumentException {
		for(MetadataGroup group : mds.getGroups()){
			if(group.getId().equals(groupName))
				return render(group);
		}
		throw new IllegalArgumentException("Group "+groupName+" was not found in the mds "+mds.getRepositoryId()+":"+mds.getId());
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
		
		for(MetadataWidget srcWidget : mds.getWidgets()){
			MetadataWidget widget=mds.findWidgetForTemplateAndCondition(srcWidget.getId(),template.getId(),properties);
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
			String widgetHtml="<div class='mdsWidget";
			if(widget.getType()!=null)
				widgetHtml+=" mdsWidget_"+widget.getType() ;
			widgetHtml+="'"+attributes+"><div class='mdsWidgetCaption'>"+widget.getCaption()+"</div>";
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
					widgetHtml+="<div class='mdsValue'>";
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
						if(link!=null)
							value+="</a>";
						value+="<div class='licenseName'>" +getLicenseName(licenseName,properties) +"</div>";

						if(CCConstants.COMMON_LICENSE_CUSTOM.equals(licenseName) && properties.containsKey(CCConstants.getValidLocalName(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION))) {
							String licenseDescription=properties.get(CCConstants.getValidLocalName(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION))[0];
							value+="<div class='licenseDescription'>"+StringEscapeUtils.escapeHtml(licenseDescription)+"</div>";
						}
						else{
							value+="<div class='licenseDescription'>" +getLicenseDescription(licenseName) +"</div>";
						}

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
						if(widget.getType().equals("checkbox")) {
							try{
								value = MetadataHelper.getTranslation(new Boolean(value) ? "boolean_yes" : "boolean_no");
							}catch(Throwable t){
								logger.info("Error parsing value "+value+" for checkbox widget "+widget.getId(),t);
							}
						}
					}
					if(valuesMap.containsKey(value))
						value=valuesMap.get(value).getCaption();

					if(widget.getFormat()!=null && !widget.getFormat().isEmpty()){
						if(widget.getFormat().contains("${value}")) {
							value = widget.getFormat().replace("${value}", value);
						}
					}
					boolean isLink=false;
					if(widget.getLink()!=null && !widget.getLink().isEmpty()){
						widgetHtml+="<a href=\""+value+"\" target=\""+widget.getLink()+"\">";
						isLink=true;
					}
					else if("vcard".equals(widget.getType())){
						try {
							HashMap<String, Object> data = VCardConverter.vcardToHashMap(value).get(0);
							value = VCardConverter.getNameForVCard("",data);
							if (data.get(CCConstants.VCARD_URL) != null) {
								String url = data.get(CCConstants.VCARD_URL).toString();
								if(!url.isEmpty()) {
									if (!url.contains("://"))
										url = "http://" + url;
									widgetHtml += "<a href=\"" + url + "\" target=\"_blank\">";
									isLink = true;
								}
							}
						}catch(Throwable t){
							// empty or invalid value
						}
					}

					widgetHtml+="<div class='mdsValue'>";
					if(widget.getIcon()!=null){
						widgetHtml+=insertIcon(widget.getIcon());
					}
					if(!value.trim().isEmpty())
						empty=false;
					widgetHtml+=value;
					widgetHtml+="</div>";
					if(isLink) {
						widgetHtml+="</a>";
					}

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
	private String getLicenseName(String licenseName, Map<String, String[]> properties) {
		if(licenseName==null || licenseName.isEmpty())
			licenseName="NONE";

		if(licenseName.startsWith(CCConstants.COMMON_LICENSE_CC_BY)) {
			licenseName=CCConstants.COMMON_LICENSE_CC_BY;
		}
		if(licenseName.equals(CCConstants.COMMON_LICENSE_PDM)){
			licenseName=CCConstants.COMMON_LICENSE_CC_ZERO;
		}
		String name=I18nAngular.getTranslationAngular("common","LICENSE.GROUPS."+licenseName);
		if(licenseName.startsWith(CCConstants.COMMON_LICENSE_CC_BY)){
			String[] version=properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION));
			String data="";
			if(version!=null && version.length>0 && version[0]!=null && !version[0].isEmpty()){
				name+=" ("+version[0];
				try{
					if(Double.parseDouble(version[0])<4.0){
						String[] locale=properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE));
						if(locale!=null && locale.length>0 && locale[0]!=null && !locale[0].isEmpty()) {
							name += " - " + I18nAngular.getTranslationAngular("common","LANGUAGE."+locale[0]);
						}
					}
				}catch(Throwable t){}
				name+=")";
			}
		}
		return name;
	}
	private String getLicenseDescription(String licenseName) {
		if(licenseName==null || licenseName.isEmpty())
			licenseName="NONE";

		if(licenseName.equals(CCConstants.COMMON_LICENSE_PDM)){
			licenseName=CCConstants.COMMON_LICENSE_CC_ZERO;
		}
		if(licenseName.startsWith(CCConstants.COMMON_LICENSE_CC_BY)){
			String result=I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION.CC_BY");
			if(licenseName.contains("SA")){
				result+="\n"+I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION.CC_SHARE_SA");
			}
			if(licenseName.contains("ND")){
				result+="\n"+I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION.CC_SHARE_ND");
			}
			if(licenseName.contains("NC")){
				result+="\n"+I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION.CC_COMMERCIAL_NC");
			}
			return result;
		}
		return I18nAngular.getTranslationAngular("common","LICENSE.DESCRIPTION."+licenseName);
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
