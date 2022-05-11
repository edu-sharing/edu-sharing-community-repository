<%@ page 	language="java" 
			contentType="text/html; charset=ISO-8859-1"
    		pageEncoding="ISO-8859-1"
    		import="org.edu_sharing.repository.client.tools.*,org.edu_sharing.repository.screenreader.*,org.edu_sharing.repository.client.rpc.*,org.edu_sharing.repository.client.auth.*,org.edu_sharing.repository.server.*,org.edu_sharing.repository.client.workspace.tools.*,org.edu_sharing.repository.client.rpc.metadataset.*,org.edu_sharing.repository.server.tools.*,
    		java.util.*"
    		
%><%
    
    if (request.getParameter("key")==null) {
    	out.write("ERROR");
    	response.flushBuffer();
    	return;
    }
    
    String key = request.getParameter("key").toUpperCase();
    
%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Index <%= key %></title>
</head>
<body>
<h1>INDEX <%= key %></h1>
<%
	// GET ALL REGISTERED DATA SOURCES / REPOSITIES
	HashMap<String, ApplicationInfo> repInfos = ApplicationInfoList.getApplicationInfos();
	RepositoryInfo repInfo = MCAlfrescoServiceAdapter.getRepositoryiesInfo();
	// HashMap<String,HashMap<String,String>> repInfoList = repInfo.getRepInfoMap();
	// ClientGlobals.setRepInfoList(repInfoList);
	ClientGlobals.setRepMetadatasets(repInfo.getRepMetadataSetsMap());		
	MCAlfrescoServiceAdapter.setHomeRepository(repInfo);

	/*
	* DO AUTHENTIFICATION
	*/

	HashMap<String, String> authInfo = null;	

	ApplicationInfo repHomeInfo = ApplicationInfoList.getRepositoryInfo(CCConstants.REPOSITORY_FILE_HOME);
	String wsUrl = repHomeInfo.getWebServiceUrl();

	try {
		// TODO: is auth OK?
		authInfo = MCAlfrescoBaseClient.createNewSession("admin", "admin", wsUrl);
	} catch (Exception e) {
		System.out.println("----------------- Exception on Authentification -------------");
		e.printStackTrace();
	}

	/*
 	* BUILD SEARCH REQUEST 
 	*/ 		
 	
	HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> searchMetaDataSet = new HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>>(); 	
	MetadataSetQuery query = new MetadataSetQuery();
	query.setParent(new MetadataSetQueries());
	HashMap<MetadataSetQueryProperty, String[]> map = new HashMap<MetadataSetQueryProperty, String[]>();
	MetadataSetQueryProperty prop = new MetadataSetQueryProperty();
	map.put(prop,null);
	searchMetaDataSet.put(query,map);
	
	
	String[] contentKinds = {CCConstants.CCM_TYPE_IO,CCConstants.CCM_TYPE_ES};

	MetadataSets mdss = ClientGlobals.getRepMetadatasets().get(ClientGlobals.getHomeRepositoryId());
	MetadataSet mds = mdss.getMetadataSetById(CCConstants.metadatasetdefault_id);
	MetadataSetQuery mdsQuery = mds.getMetadataSetQueries().getMetadataSetQueries().get(0);

	SearchCriterias searchCriterias = new SearchCriterias();
	searchCriterias.setSearchWord("TITLE:"+key.toLowerCase());
	searchCriterias.setContentkind(contentKinds);
	searchCriterias.setMetadataSetSearchData(searchMetaDataSet);	
	
	SearchToken searchToken = new SearchToken();
	// searchToken.setAuthenticationInfo(authInfo);
	searchToken.setSearchCriterias(searchCriterias);
	searchToken.setStartIDX(0);
	searchToken.setNrOfResults(99999999);
	searchToken.setRepositoryId(ClientGlobals.getHomeRepositoryId());

	/*
 	* DO SEARCH
 	*/ 	

	SearchResult result = null;
	try{
		MCBaseClient mcBaseClient = RepoFactory.getInstanceForRepo(ApplicationInfoList.getRepositoryInfoById(searchToken.getRepositoryId()), authInfo);
		result = mcBaseClient.search(searchToken);	
	}catch(Throwable e){
		System.out.println("FAILED on listing search");		
		e.printStackTrace();
	}

	if (result==null) {
		System.out.println("Listing Searchresult is NULL");
		out.println("ERROR on repo request.");
	} else {
		 	
 		// loop thru results
		int counter = result.getStartIDX();
 		int realResultsCounter = 0;
		Iterator<String> k = result.getData().keySet().iterator();
		while (k.hasNext()) {
		
	counter++;		
	out.print("\n<!-- ITEM "+counter+" -->\n");		
		
	String keyStr = k.next();
	HashMap<String, Object> data = result.getData().get(keyStr);
	
	try {
		String title = (String)data.get(CCConstants.CM_PROP_TITLE);
		if (title!=null) {
			
			// check if title begins with key
			if (title.toLowerCase().startsWith(key.toLowerCase())) {
				realResultsCounter++;
				out.println("<br/>"+title);						
			}
			
		} else {
			out.println("<br/>ZERO");				
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	
	// loop thru data items of result
	//System.out.println("+-- "+counter+" -----------");
	Iterator<String> p = data.keySet().iterator();
	String disabledItemsNote = "";
	while (p.hasNext()) {
		String fieldKey = p.next();
		//System.out.println(fieldKey);
	}

		}
		
		if (realResultsCounter==0) {
	out.println("<br/>No results.");
		}
	}
%>
</body>
</html>