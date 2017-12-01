<%@ page 	language="java" 
			contentType="text/html; charset=ISO-8859-1"
    		pageEncoding="ISO-8859-1"
    		import="org.edu_sharing.repository.client.tools.*,org.edu_sharing.repository.screenreader.*,org.edu_sharing.repository.server.tools.*,org.edu_sharing.repository.server.*,org.edu_sharing.repository.client.rpc.*,org.edu_sharing.repository.client.workspace.tools.*,
    		org.edu_sharing.metadataset.*,org.edu_sharing.repository.client.tools.metadata.*,org.edu_sharing.repository.client.rpc.metadataset.*,	
    		java.util.*" 		  		
%>
<% 
	/*
 	* GET LANGUAGE DATA
 	*/

	// set language by default or use setting by url parameter "lang"
	Locale locale = JspTools.getLocaleFromRequest(request);

	ResourceBundle text = ResourceBundle.getBundle("org.edu_sharing.metadataset.CCSearchI18n", locale);	

	/*
	 * GET SPECIAL CONFIGURATIONS
	 */
	 
	String customCSS = "";
	try {
		Properties props = PropertiesHelper.getProperties(Const.PROPERTIES_FILE,PropertiesHelper.TEXT);
		customCSS = props.getProperty(Const.CUSTOM_CSS_PATH);
	} catch (Exception e) {
		System.err.println("ResultDisplayConfigurator: Was not able to load properties from '"+Const.PROPERTIES_FILE+"'");			
	} 
	

	/*
	 * GET PARAMETERS
	 */	
	 
	String detailKey = request.getParameter("detail");
	if ((detailKey==null) || (detailKey.equals(""))) { 
		String msg = text.getString("scr_error_1");
		%><jsp:forward page="error.jsp"><jsp:param name="msg" value="<%=msg%>" /></jsp:forward><%
	}		
	
	if (session.getAttribute(Const.SESSION_SEARCH_RESULT_BUFFER)==null) { 
		String msg = text.getString("scr_error_5");
		%><jsp:forward page="error.jsp"><jsp:param name="msg" value="<%=msg%>" /></jsp:forward><%
	}		
	
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="org.edu_sharing.repository.client.rpc.SearchResult"%><html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title><%= text.getString("scr_index_title") %></title>
	<link rel="stylesheet" type="text/css" href="stylesheet.css">
	<%  
		if ((customCSS!=null) && (customCSS.length()>2)) {
			%><link rel="stylesheet" type="text/css" href="<%= customCSS %>"><%
		}
	%>
		
</head>
<body>


<!-- <img src="/<%=request.getContextPath() %>/ccimages/edulogo.jpg" alt="edu sharing Logo" width="164" height="52"> -->

<h1><%= text.getString("scr_details_headline") %></h1>

<%

	// GET ALL REGISTERED DATA SOURCES / REPOSITIES
	HashMap<String, ApplicationInfo> repInfos = ApplicationInfoList.getApplicationInfos();
	RepositoryInfo repInfo = MCAlfrescoServiceAdapter.getRepositoryiesInfo();
	// HashMap<String,HashMap<String,String>> repInfoList = repInfo.getRepInfoMap();
	// ClientGlobals.setRepInfoList(repInfoList);
	ClientGlobals.setRepMetadatasets(repInfo.getRepMetadataSetsMap());		
	MCAlfrescoServiceAdapter.setHomeRepository(repInfo);

	// init html renderer
	MetadataHtmlRenderer metadataHtmlRenderer = new MetadataHtmlRenderer(text);

	// get search data from session
	HashMap<String, HashMap<String, Object>> result = (HashMap<String, HashMap<String, Object>>) session.getAttribute(Const.SESSION_SEARCH_RESULT_BUFFER);
	HashMap<String, Object> properties = result.get(detailKey);
	
	// production
	String nodeType = (String)properties.get(CCConstants.NODETYPE);
	String repositoryId = (String)properties.get(CCConstants.REPOSITORY_ID);
	
	String metadatasetId = (String) properties.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
	metadatasetId = (metadatasetId == null) ? CCConstants.metadatasetdefault_id : metadatasetId;
	
	MetadataSetView mdsv = MetadataTool.getMetadataSetView(repositoryId,metadatasetId, nodeType);	
	
	out.print("<p>"+text.getString("scr_details_description")+"</p>");
	
	DetailDisplayConfigurator detailDisplayConfigurator = new DetailDisplayConfigurator();
	
	for(MetadataSetViewProperty mdsvprop: mdsv.getProperties()){
		
		String key = mdsvprop.getName();
		String value = null;
		try { value = (String) properties.get(key); } catch (Exception e) { e.printStackTrace(); }
		if ((value==null) || (value.equals(""))) continue;
		
		String label = null;	
		String labelKey = null;
		try {
			labelKey = (mdsvprop.getLabel() == null)? "" : mdsvprop.getLabel().getKey();
			label = text.getString(labelKey);
		} catch (Exception e) {
			out.print("\n <!-- No translation for key ("+key+") -->");			
		}
		
		if ((label!=null) && (label.length()>1)) {
			
			// make first letter on label upper case
			label = label.substring(0,1).toUpperCase()+label.substring(1);
			
			// take care of special formating on value
			value = metadataHtmlRenderer.renderHtmlForDetailView(key, value);
			
			if (!detailDisplayConfigurator.addHtmlViewOfItem(key, label, value)) {
			 	out.print("\n<!--  data with key '"+key+"' gets not diplayed by configuration -->");
			}
		}		
	}
	
	// print detail view
	out.println(detailDisplayConfigurator.constructResultItemHTML());
	
	// download URL
	String contentUrl = MCAlfrescoServiceAdapter.getContectURL(properties);
	out.print("<p><div class='result_download'><a href='"+contentUrl+"' target='_blank' title='"+text.getString("scr_results_downloaddetail")+"'>Download "+detailDisplayConfigurator.getTitleString()+"</a></div></p>");	
%>
<p>
<div onclick="javascript:history.back()"><%= text.getString("scr_details_back") %><div>
</p>
</body>
</html>