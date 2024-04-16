<%@ page 	language="java" 
			contentType="text/html; charset=ISO-8859-1"
    		pageEncoding="ISO-8859-1"
    		import="org.edu_sharing.repository.client.tools.*,org.edu_sharing.repository.screenreader.*,org.edu_sharing.repository.server.tools.*,    		
    		java.util.*"
    		  		
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="java.util.Iterator"%>
<%@page import="java.net.URLEncoder"%>
<% 
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

%>
<html>

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


<!-- <img src="/<%=request.getContextPath() %>/ccimages/edulogo.jpg" alt="edu sharing Logo" width="164" height="52">  -->

<h1><%= text.getString("scr_error_headline") %></h1>

<p><%= text.getString("scr_error_intro") %>:</p>

<p id="beschreibung"><%= request.getParameter("msg") %></p>

<%  if (request.getParameterMap().size()>2) { %>
<a href="index.jsp?<%= JspTools.reCreateParameterString(request) %>"><%= text.getString("scr_results_refine") %></a> <br />
<%  }  %>

<a href="index.jsp?lang=<%=locale.toString()%>"><%= text.getString("scr_results_new") %></a>

</body>
</html>