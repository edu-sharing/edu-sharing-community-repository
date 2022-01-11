<%@ page 	language="java" 
			contentType="text/html; charset=ISO-8859-1"
    		pageEncoding="ISO-8859-1"
    		import="org.edu_sharing.repository.client.tools.*,org.edu_sharing.repository.screenreader.*,org.edu_sharing.repository.client.rpc.*,org.edu_sharing.repository.client.auth.*,org.edu_sharing.repository.server.*,org.edu_sharing.repository.client.workspace.tools.*,org.edu_sharing.repository.client.rpc.metadataset.*,org.edu_sharing.repository.server.tools.*,org.edu_sharing.repository.client.tools.metadata.*,
    		java.util.*,org.edu_sharing.repository.client.tools.metadata.search.*"
    		
%><%

	// set language by default or use setting by url parameter "lang"
	Locale locale = JspTools.getLocaleFromRequest(request);

	ResourceBundle text = ResourceBundle.getBundle("org.edu_sharing.metadataset.CCSearchI18n", locale);

	/*
	 * CHECK OF PARAMETERS 
	 */ 

	// start item (paging)
	int startItem = 1;
	try { 
		String startItemStr = request.getParameter(Const.PARA_STARTITEM);
		if (startItemStr==null) startItemStr="1";	
		startItem = new Integer(startItemStr).intValue(); 
	} catch (Exception e) {e.printStackTrace();}
	 
	// search word
	String searchWord = request.getParameter(Const.PARA_SUCHANFRAGE);
	if (searchWord==null) { 
		String msg = text.getString("scr_error_1");		
		%> <jsp:forward page="error.jsp"><jsp:param name="msg" value="<%=msg%>" /></jsp:forward><%
	}
	if (searchWord.equals("")) { 
		String msg = text.getString("scr_error_2");
		%> <jsp:forward page="error.jsp"><jsp:param name="msg" value="<%=msg%>" /></jsp:forward><%
	}

	/*
	 * GET SEARCH META DATA SET
	 */ 	
	 
	// get repositor & metadata
	String repoId = request.getParameter(Const.PARA_REPO);
	if ((repoId==null) ) { 
		String msg = text.getString("scr_error_4");			
		%> <jsp:forward page="error.jsp"><jsp:param name="msg" value ="<%=msg%>" /></jsp:forward><%
	}	
			  
	SearchMetadataHandler metadataHandler = new SearchMetadataHandler(repoId);
	HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> searchMetaDataSet = null;
	
	 try {
		 searchMetaDataSet = metadataHandler.getSearchData(request); 
	 } catch (MetadataMailformedException mme) {
		 %> <jsp:forward page="error.jsp"><jsp:param name="msg" value ="<%= mme.getMessage() %>" /></jsp:forward><%		 
	 }
	 
	/*
	 * BUILD SEARCH REQUEST 
	 */ 			 
	 
	MetadataSetQueryProperty searchWordProp = MetadataTool.getMetadataSetQueryProperty(ClientGlobals.getHomeRepositoryId(), CCConstants.metadatasetdefault_id, MetadataSetQuery.DEFAULT_CRITERIABOXID, MetadataSetQueryProperty.PROPERTY_NAME_CONSTANT_searchword);
	if (searchWordProp==null) {
		String msg = "SETUP ERROR - Metadataset for Screenreader not able to set ("+Const.METADATASET_SCREENREADER_PATH+")";		
		%> <jsp:forward page="error.jsp"><jsp:param name="msg" value ="<%=msg%>" /></jsp:forward><%
	} 
	SearchMetadataHelper smdhelper = new SearchMetadataHelper();
	HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> searchData = smdhelper.createSearchData(searchWordProp, new String[]{searchWord});
	SearchCriterias sc = new SearchCriterias();
	sc.setMetadataSetSearchData(searchData);
	sc.setContentkind(new String[]{CCConstants.CCM_TYPE_IO,CCConstants.CCM_TYPE_MAP});
	sc.setMetadataSetId("default");
	sc.setRepositoryId(ClientGlobals.getHomeRepositoryId());
	  
	SearchToken st = new SearchToken();
	st.setSearchCriterias(sc);
	st.setRepositoryId(sc.getRepositoryId());
	st.setStartIDX(0);
	st.setNrOfResults(10);
	 		
 	/*
 	 * DO SEARCH
 	 */ 	
 	
 	SearchResult result = null;
 	try{
 		MCBaseClient mcBaseClient = (MCBaseClient)RepoFactory.getInstance(st.getRepositoryId(),  session);
 		result = mcBaseClient.search(st);
 		System.out.println("Search process mcBaseClient finished OK");			
 	}catch(Throwable e){
 		System.out.println("FAILED");		
 		e.printStackTrace();
 	}
 	if (result==null) {
 		System.out.println("Searchresult is NULL");
 %> <jsp:forward page="error.jsp"><jsp:param name="msg" value ="Es gab leider ein serverseitiges Problem" /></jsp:forward><%
	} else {
		// buffer in session for detail view
		session.setAttribute(Const.SESSION_SEARCH_RESULT_BUFFER,result.getData());
	}
	
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
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
  	<title><%= text.getString("scr_index_title") %></title>
  	<meta name="description" content="<%= text.getString("scr_results_desc") %>"/>
	<link rel="stylesheet" type="text/css" href="stylesheet.css">
	<style type="text/css">
	h2 {
		font-size: 90%;
		padding-top: 10px;
		padding-bottom: 0px;			
	}		
	</style>
	<%  
		if ((customCSS!=null) && (customCSS.length()>2)) {
			%><link rel="stylesheet" type="text/css" href="<%= customCSS %>"><%
		}
	%>
</head>

<body>

<!-- <img src="/<%=request.getContextPath() %>/ccimages/edulogo.jpg" alt="edu sharing Logo" width="164" height="52"> -->

<h1><%= text.getString("scr_results_headline") %></h1>

<p>
	<% if (result.getNodeCount()==0) { %>		
		<%= text.getString("scr_results_noresults") %>
	<% } else if (result.getNodeCount()==1) { %>		
		<%= text.getString("scr_results_oneresult") %>
	<% } else { %>
		<%= result.getNodeCount() %> <%= text.getString("scr_results_results") %><br/>
	<% }%>			
	<% if (result.getNodeCount()>=2) {
		int bis = startItem+9;
		if (result.getNodeCount()>10) {
			if (bis>result.getNodeCount()) bis = result.getNodeCount(); 
			%>	
			<%= text.getString("scr_results_listing") %> <%= startItem %> <%= text.getString("scr_results_until") %> <%= bis %>:
		<% }%>				
	<% }%>		
</p>

<%
	/*
 	* OUTPUT RESULTS 
 	*/ 
	
 	// prepare config and html redering
	ResultDisplayConfigurator resultDisplayConfigurator = new ResultDisplayConfigurator();	
	MetadataHtmlRenderer metadataHtmlRenderer = new MetadataHtmlRenderer(text);
 	
 	// loop thru results
	int counter = result.getStartIDX();
	Iterator<String> k = result.getData().keySet().iterator();
	while (k.hasNext()) {
		
		counter++;		
		out.print("\n<!-- ITEM "+counter+" -->\n");		
		
		String key = k.next();
		HashMap<String, Object> data = result.getData().get(key);
		
		// loop thru data items of result
		//System.out.println("+-- "+counter+" -----------");
		Iterator<String> p = data.keySet().iterator();
		String disabledItemsNote = "";
		while (p.hasNext()) {
	
	// get item key
	String metadataItemKey = p.next();
	//System.out.println(metadataItemKey);
	

	// get html redering and add to Configurator
	String html = metadataHtmlRenderer.renderHtmlForSearchResults(metadataItemKey, data);
	resultDisplayConfigurator.addHtmlViewOfItem(metadataItemKey, html, data);
	
	// wirte comment about disabled data
	if (disabledItemsNote.length()>1) {
		out.print("<!-- Not displayed on preview: "+disabledItemsNote.substring(1)+" -->");
	}
		}
		
		// get data to display
		String contentUrl = MCAlfrescoServiceAdapter.getContectURL(data);
		out.println(resultDisplayConfigurator.constructResultItemHTML(counter+""));
		
		// add download and details link
		out.print("<div class='result_detail'><a href='detail.jsp?detail="+key+"' title='"+text.getString("scr_results_detaildetail")+"'>"+counter+". "+text.getString("scr_results_detail")+" "+resultDisplayConfigurator.getTitle()+"</a></div>");		
		out.print("<div class='result_download'><a href='"+contentUrl+"' target='_blank' title='"+text.getString("scr_results_downloaddetail")+"'>"+counter+". "+text.getString("scr_results_download")+" "+resultDisplayConfigurator.getTitle()+"</a></div>");
	}
%>
	
<h2><%= text.getString("scr_results_options") %></h2>
<map title="<%= text.getString("scr_results_options") %>"> 
<% if (result.getNodeCount()>=10) {%>
<p>
<% if (result.getNodeCount()>=(startItem+10)) {%>
	<a href="search.jsp?<%= JspTools.reCreateParameterString(request) %>&<%= Const.PARA_STARTITEM %>=<%= startItem+10 %>" title="<%= text.getString("scr_results_nextdetail") %>"><%= text.getString("scr_results_next") %></a><br/>
<% } %>
<% if (startItem>=11) {%>
	<a href="search.jsp?<%= JspTools.reCreateParameterString(request) %>&<%= Const.PARA_STARTITEM %>=<%= startItem-10 %>" title="<%= text.getString("scr_results_backdetail") %>"><%= text.getString("scr_results_back") %></a><br/> 
<% } %>
</p>
<% } %>
<a href="index.jsp?<%= JspTools.reCreateParameterString(request) %>" title="<%= text.getString("scr_results_refinedetail") %>"><%= text.getString("scr_results_refine") %></a><br/>
<a href="index.jsp?lang=<%=locale.toString()%>" title="<%= text.getString("scr_results_newdetail") %>"><%= text.getString("scr_results_new") %></a>
</map>
</body>
</html>