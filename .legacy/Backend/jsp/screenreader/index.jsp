<%@ page 	language="java" 
			contentType="text/html; charset=ISO-8859-1"
    		pageEncoding="ISO-8859-1"
    		import="org.edu_sharing.repository.client.tools.*,org.edu_sharing.repository.screenreader.*,org.edu_sharing.repository.server.tools.*,org.edu_sharing.repository.server.*,org.edu_sharing.repository.client.rpc.*,org.edu_sharing.repository.client.workspace.tools.*,
    		org.edu_sharing.metadataset.*,    		
    		java.util.*"
%><%

	// set language by default or use setting by url parameter "lang"
	Locale locale = JspTools.getLocaleFromRequest(request);
	ResourceBundle text = ResourceBundle.getBundle("org.edu_sharing.metadataset.CCSearchI18n", locale);

	// GET ALL REGISTERED DATA SOURCES / REPOSITIES
	HashMap<String, ApplicationInfo> repInfos = ApplicationInfoList.getApplicationInfos();
	RepositoryInfo repInfo = MCAlfrescoServiceAdapter.getRepositoryiesInfo();
	ClientGlobals.setRepMetadatasets(repInfo.getRepMetadataSetsMap());		
	MCAlfrescoServiceAdapter.setHomeRepository(repInfo);
	
	// Init TabIndex
	int tabIndex = 3;
	
	// get metadata fieldsq
	SearchMetadataHandler metadataHandler = new SearchMetadataHandler(ClientGlobals.getHomeRepositoryId());	
	
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
	
%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
  	<title><%= text.getString("scr_index_title") %></title>
  	<meta name="description" content="<%= text.getString("scr_index_desc") %>" />
	<link rel="stylesheet" type="text/css" href="stylesheet.css" />
	<%  
		if ((customCSS!=null) && (customCSS.length()>2)) {
			%><link rel="stylesheet" type="text/css" href="<%= customCSS %>"><%
		}
	%>	
</head>

<body>


<!-- 
<img src="/<%=request.getContextPath() %>/ccimages/edulogo.jpg" alt="edu sharing Logo" width="164" height="52">
 -->

<h1><%= text.getString("scr_search_headline") %></h1>

<p><%= text.getString("scr_search_intro") %></p>

<form action="search.jsp" method="post" name="suchmaske" id="suchmaske">

	<input type="hidden" name="lang" value="<%= locale.toString() %>" />
	<!--  SUCHE BASIC -->
	
	<fieldset>
		<legend class="mainfieldset"><%= text.getString("scr_form_searchword") %></legend>
		<label for="<%= Const.PARA_SUCHANFRAGE %>"><%= text.getString("scr_form_searchword_label") %>:</label>
		<input tabindex="1" type="text" title="<%= text.getString("scr_form_searchword_title") %>" <%= JspTools.text_nameValue(request,Const.PARA_SUCHANFRAGE,"") %> size="30"/>
		<input tabindex="2" name="sendrequest" type="submit" value= "<%= text.getString("scr_form_send") %>" />
	</fieldset><br/>
	
	<!--  SUCHE WEITERE PARAMETER -->
	
	<fieldset>
	<legend class="mainfieldset"><%= text.getString("scr_form_extpara_headline") %></legend>
	
	<p><%= text.getString("scr_form_extpara_intro") %>:</p>
				
	<%= metadataHandler.getHtmlFormInputs(locale.toString(), tabIndex, 0, request) %>	
	<input name="sendrequest" type="submit" value="<%= text.getString("scr_form_send") %>" />	
	</fieldset>
</form>
<p>

<a href="http://www.achecker.ca/checker/index.php" title="Web Accessibility Check">
<img src="http://www.achecker.ca/images/icon_W2_aa.jpg" alt="" height="32" width="102"  border="0" />
</a>
</p>
</body>
</html>