package org.edu_sharing.repository.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.SearchCriterias;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.rpc.SearchToken;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.search.SearchMetadataHelper;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MCDDBClient extends MCBaseClient {
	
	final String baseUrl = "https://api.deutsche-digitale-bibliothek.de";
	
	String apiKey = null;
	
	Logger logger = Logger.getLogger(MCDDBClient.class);
	
	String repositoryId = null;
	public MCDDBClient(String repoFile, HashMap authInfo) {
		if(repoFile != null){
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfo(repoFile);
			this.repositoryId  = appInfo.getAppId();
			apiKey = appInfo.getApiKey();
		}
	}
	
	public static String httpGet(String urlStr, HashMap<String, String> properties) throws IOException {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
        // open HTTP connection with URL
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // set properties if any do exist
        for (String key : properties.keySet()) {
            conn.setRequestProperty(key, properties.get(key));
        }
        // test if request was successful (status 200)
        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }
        // buffer the result into a string
        InputStreamReader isr = new InputStreamReader(conn.getInputStream(), "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        isr.close();
        conn.disconnect();
        return sb.toString();
    }
	
	String query(String queryParams, String offset, String rows) throws UnsupportedEncodingException, IOException{
		String url = baseUrl+"/search";
		url = url + "?oauth_consumer_key=" + URLEncoder.encode(apiKey, "UTF-8");
		url = url + "&"+queryParams;
		url = url + "&offset="+offset;
		url = url + "&rows=" +rows;
		String queryJsonResult = httpGet(url, null);
        return queryJsonResult;
	}
	
	@Override
	public SearchResult search(SearchToken searchToken) throws Throwable {

		HashMap<String,String> queryParams = this.getQueryString(searchToken.getSearchCriterias().getMetadataSetSearchData());
		
		String queryParamsString = null;
		for(Map.Entry<String, String> queryParam : queryParams.entrySet()){	
			if(queryParamsString == null){
				queryParamsString = "";
			}else{
				queryParamsString = "&";
			}
			queryParamsString = queryParam.getKey() + "=" + URLEncoder.encode(queryParam.getValue(), "UTF-8");
		}
		
		String jsonString = this.query(queryParamsString, ""+searchToken.getStartIDX(), ""+searchToken.getNrOfResults());
		JSONObject jo = (JSONObject)new JSONParser().parse(jsonString);
    	
		Long nrOfResult = (Long)jo.get("numberOfResults");
		JSONArray resultsArr = (JSONArray)jo.get("results");
		JSONObject results = (JSONObject)resultsArr.get(0);
		JSONArray docs = (JSONArray)results.get("docs");
		
		HashMap<String,HashMap<String,Object>> result = new HashMap<String, HashMap<String,Object>>();
		for(int i = 0; i < docs.size(); i++){
			JSONObject doc = (JSONObject)docs.get(i);
			HashMap<String,Object> props = getProperties(doc);
			result.put((String)props.get(CCConstants.SYS_PROP_NODE_UID), props);
		}
    	
		org.edu_sharing.repository.client.rpc.SearchResult sr = new org.edu_sharing.repository.client.rpc.SearchResult();
		
		sr.setSearchCriterias(searchToken.getSearchCriterias());

		sr.setStartIDX(searchToken.getStartIDX());
	
		sr.setData(result);
		sr.setNodeCount(new Long(nrOfResult).intValue());
		   	
		return sr;
	}
	
	public HashMap<String,HashMap<String,Object>> search(String queryParamsString, int from, int nrOfResults) {
		HashMap<String,HashMap<String,Object>> result = new HashMap<String, HashMap<String,Object>>();
		
		try {
			String jsonString = this.query(queryParamsString, ""+from, ""+nrOfResults);
			JSONObject jo = (JSONObject)new JSONParser().parse(jsonString);
	    	
			Long nrOfResult = (Long)jo.get("numberOfResults");
			JSONArray resultsArr = (JSONArray)jo.get("results");
			JSONObject results = (JSONObject)resultsArr.get(0);
			JSONArray docs = (JSONArray)results.get("docs");
			
			for(int i = 0; i < docs.size(); i++){
				JSONObject doc = (JSONObject)docs.get(i);
				HashMap<String,Object> props = getProperties(doc);
				result.put((String)props.get(CCConstants.SYS_PROP_NODE_UID), props);
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return result;
	}
	
	@Override
	public SearchResult searchRecommend(int startIdx, int nrOfResults)
			throws Throwable {
		SearchToken st = new SearchToken();
		st.setStartIDX(startIdx);
		st.setNrOfResults(nrOfResults);

		SearchCriterias sc = new SearchCriterias();
		String searchword = ApplicationInfoList.getRepositoryInfoById(this.repositoryId).getRecommend_objects_query();
		if (searchword == null || searchword.trim().equals(""))
			searchword = "Mathematik";
		HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> sd = new SearchMetadataHelper().createSearchData("searchword", "",
				new String[] { searchword });
		sd.keySet().iterator().next().setCriteriaboxid(MetadataSetQuery.DEFAULT_CRITERIABOXID);
		sc.setMetadataSetSearchData(sd);
		sc.setRepositoryId(this.repositoryId);
		st.setSearchCriterias(sc);

		return search(st);
	}
	
	private HashMap<String,Object> getProperties(JSONObject doc){
		HashMap<String,Object> properties = new  HashMap<String,Object>();
		String id = (String)doc.get("id");
		String title = (String)doc.get("label");
		String thumbnail = (String)doc.get("thumbnail");
		
		properties.put(CCConstants.LOM_PROP_GENERAL_TITLE, title);
		properties.put(CCConstants.SYS_PROP_NODE_UID, id);
		//properties.put(CCConstants.CONTENTURL, esrContentUrl);
		try{
			
			String thumbnailUrl = baseUrl+ thumbnail+"?oauth_consumer_key=" + URLEncoder.encode(apiKey, "UTF-8");
			//for the importer
			properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, thumbnailUrl);	
			//for the gwt gui no persistent
			properties.put(CCConstants.CM_ASSOC_THUMBNAILS, thumbnailUrl);
			
		}catch(UnsupportedEncodingException e){}
		properties.put(CCConstants.REPOSITORY_ID, this.repositoryId );
		properties.put(CCConstants.NODETYPE, CCConstants.CCM_TYPE_IO);
		
		properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, "");
		
		try{
			properties.putAll(getProperties(id));
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
		}
		
		// CLEANUP?
		/*properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, esrTechLoc);
		properties.put(CCConstants.CM_PROP_C_MODIFIED, esrModeified);
		
		if(user != null){
			properties.put(CCConstants.CM_PROP_C_MODIFIER, user);
		}
		
		if(keywords != null){
			properties.put(CCConstants.LOM_PROP_GENERAL_KEYWORD,keywords);
		}

		properties.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, esrDescr);

		properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "video/x-flash");
		properties.put(CCConstants.NODETYPE, CCConstants.CCM_TYPE_IO);
		*/
		
		return properties;
	}
	
	
	// CLEANUP?
	public static void main(String[] args) throws IOException {
		
		try{
			String result = new MCDDBClient(null, null).query("query=Brillengläsern","0","30");
			System.out.println(result);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if(true) return;
		
		 
        // URL of DDB server with dataset ID and requested method
        final String url = "https://api.deutsche-digitale-bibliothek.de/items/OAXO2AGT7YH35YYHN3YKBXJMEI77W3FF/view";
        
        String apiKey = "test1223";
         
        // get XML data via HTTP request header authentication
        String httpXmlResult = httpGet(url, new HashMap<String, String>() {
            {
                put("Authorization", "OAuth oauth_consumer_key=\"" + apiKey + "\"");
                put("Accept", "application/xml");
            }
        });
        System.out.println(httpXmlResult); // print results
         
        // get JSON data via HTTP request header authentication
        String httpJsonResult = httpGet(url, new HashMap<String, String>() {
            {
                put("Authorization", "OAuth oauth_consumer_key=\"" + apiKey + "\"");
                put("Accept", "application/json");
            }
        });
        System.out.println(httpJsonResult); // print results
         
        // get JSON data via query parameter authentication
        // remember: use URL encoded Strings online -> URLEncoder.encode(s, enc)
        String queryJsonURL = url + "?oauth_consumer_key=" + URLEncoder.encode(apiKey, "UTF-8");
        String queryJsonResult = httpGet(queryJsonURL, null);
        System.out.println(queryJsonResult); // print results
    }

	
	
	@Override
	public void addAspect(String nodeId, String aspect) throws Exception {		
	}
	
	@Override
	public void createAssociation(String fromID, String toID, String association) {
	}
	
	@Override
	public String createNode(String parentID, String nodeTypeString,
			HashMap<String, Object> _props) throws Exception {
		return null;
	}
	
	@Override
	public String createNode(String parentID, String nodeTypeString,
			String childAssociation, HashMap<String, Object> _props)
			throws Exception {
		return null;
	}
	
	@Override
	public void createVersion(String nodeId, HashMap _properties)
			throws Exception {
	}
	
	@Override
	public List<String> getAssociationNodeIds(String nodeID, String association)
			throws Exception {
		return null;
	}
	
	@Override
	public HashMap<String, Object> getChild(String parentId, String type,
			String property, String value) throws Throwable {
		return null;
	}
	
	@Override
	public HashMap<String, HashMap<String, Object>> getChildrenByType(
			String nodeId, String type) throws Exception {
		return null;
	}
	
	@Override
	public String getDetailsHtmlSnippet(String nodeId) throws Exception {
		return null;
	}
	
	@Override
	public String getHomeFolderID(String username) throws Exception {
		return null;
	}
	
	@Override
	public MCBaseClient getInstance(HashMap<String, String> _authenticationInfo) {
		return new MCDDBClient(null, _authenticationInfo);
	}
	
	@Override
	public MCBaseClient getInstance(String _repositoryFile,
			HashMap<String, String> _authenticationInfo) {
		return new MCDDBClient(_repositoryFile, _authenticationInfo);
	}
	
	@Override
	public HashMap<String, Object> getProperties(String nodeId) throws Throwable {
		
		HashMap<String,Object> properties = new  HashMap<String,Object>();
		try{
			String result = httpGet(baseUrl+"/items/"+nodeId+"/binaries?oauth_consumer_key=" + URLEncoder.encode(apiKey, "UTF-8"), null);
			if(result != null && result.trim().length() > 0){
			
				JSONObject jo = (JSONObject)new JSONParser().parse(result);
				if(jo != null){	
					
					JSONArray binaries = (JSONArray)jo.get("binary");
					
					JSONObject binary = null;
					JSONObject binaryFallback = null;
					
					for(int i = 0; i < binaries.size();i++){
						
						binary =  (JSONObject)binaries.get(0);
						if(i == 0) binaryFallback = binary;
						
						String path = (String)binary.get("@path");
						
						// prefer mvpr
						if(path.contains("mvpr")){
							break;
						}
					}
					
					if(binary == null){
						binary = binaryFallback;
					}
					String contenturl  = (String)binary.get("@path");
					String mimetyp = (String)binary.get("@mimetype");
					contenturl = baseUrl+contenturl+"?oauth_consumer_key=" + URLEncoder.encode(apiKey, "UTF-8");
					
					//properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, contenturl);
					String url = "https://www.deutsche-digitale-bibliothek.de/item/"+nodeId;
					properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, url);
					properties.put(CCConstants.CCM_PROP_IO_WWWURL, url);
					
					properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, mimetyp);
				}
				
				properties.put(CCConstants.CONTENTURL, this.getRedirectServletLink(this.repositoryId, nodeId));
			}
			
			String items = httpGet(baseUrl+"/items/"+nodeId+"/view?oauth_consumer_key=" + URLEncoder.encode(apiKey, "UTF-8"), null);
			if(items != null && items.trim().length() > 0){
				JSONObject jo = (JSONObject)new JSONParser().parse(items);
				JSONObject item = (JSONObject)jo.get("item");
				if(item != null){
					JSONObject license = (JSONObject)item.get("license");
					//@TODO map: we dont have public domain at the moment
					JSONArray fields = (JSONArray)item.get("fields");
					
					for(int i = 0; i < fields.size();i++){
						JSONObject fieldsObj = (JSONObject)fields.get(i);
						String usage = (String)fieldsObj.get("@usage");
						if("index".equals(usage)){
							JSONArray entries = (JSONArray)fieldsObj.get("field");
							for(int f = 0; f < entries.size();f++){
								JSONObject entry = (JSONObject)entries.get(f);
								String name = (String)entry.get("name");
								
								Object val = entry.get("value");
								String value = "";
								if(val instanceof String){
									value = (String) val;
								}else if(val instanceof JSONArray){
									Object tmp = ((JSONArray)val).get(0);
									if(tmp instanceof String){
										value = (String)tmp;
									}
									if(tmp instanceof JSONObject){
										value = tmp.toString();
									}
								}else{
									logger.error("unknown type for name:"+name+" val:"+val );
								}
								
								String fid = (String)entry.get("@id");
								
								if("description".equals(fid)){
									properties.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, value);
								}
								
								if("license".equals(fid)){
									
								}
							}
						}
						if("display".equals(usage)){
							JSONArray entries = (JSONArray)fieldsObj.get("field");
							for(int f = 0; f < entries.size();f++){
								JSONObject entry = (JSONObject)entries.get(f);
								String name = (String)entry.get("name");
								
								Object val = entry.get("value");
								String value = "";
								if(val instanceof String){
									value = (String) val;
								}else if(val instanceof JSONArray){
									Object tmp = ((JSONArray)val).get(0);
									if(tmp instanceof String){
										value = (String)tmp;
									}
									if(tmp instanceof JSONObject){
										value = tmp.toString();
									}
									
								}else{
									logger.error("unknown type for name:"+name+" val:"+val );
								}
								
								String fid = (String)entry.get("@id");
								
								if("Sprache".equals(name)){
									properties.put(CCConstants.LOM_PROP_GENERAL_LANGUAGE, value);
								}
								if("Dokumenttyp".equals(name)){
									properties.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, value);
								}
								
							}
						}
						
					}
					
					JSONObject institution = (JSONObject)item.get("institution");
					if(institution != null){
						String instName = (String)institution.get("name");
						properties.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR+"FN", instName);
						
					}
					
				}
			
			}
			
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return properties;
	}
	
	@Override
	public HashMap<String, String> getUserInfo(String userName)
			throws Exception {
		return null;
	}
	
	@Override
	public boolean hasContent(String nodeId, String contentProp)
			throws Exception {
		return false;
	}
	
	@Override
	public boolean hasPermissions(String nodeId, String[] permissions) throws Exception {
		if(permissions != null && permissions[0].equals(CCConstants.PERMISSION_CC_PUBLISH)){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void removeAssociation(String fromID, String toID, String association)
			throws Exception {
	}
	
	@Override
	public void removeNode(String nodeID, String fromID) throws Exception {	
	}
	
	
	public HashMap<String,String> getQueryString(HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> map){
		
		HashMap<String,String> result = new HashMap<String,String>();
		if(map == null) return result;
		for(Map.Entry<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> entry:map.entrySet()){
			MetadataSetQuery mdsq = entry.getKey();
			
			if(mdsq != null && mdsq.getCriteriaboxid() != null){
				String criteriaboxid = mdsq.getCriteriaboxid();
				
				//quickfix for lvr 110511
				if(criteriaboxid.equals("topAreaView")){
					String[] searchWords = getValue("searchword",entry.getValue());
					if(searchWords != null){
						
						String sws = null;
						for(String sw : searchWords){
							if(sw != null && !sw.trim().equals("")){
								if(sws == null){
									sws = sw;
								}else{
									sws += " "+sw;
								}
							}
							
						}
					
						result.put("query", sws);
					}					
				}
			}
		}
		return result;
	}
public String[] getValue(String propName, HashMap<MetadataSetQueryProperty, String[]> propValue){
	for(Map.Entry<MetadataSetQueryProperty, String[]> entry: propValue.entrySet()){
		if(propName.equals(entry.getKey().getName())){
			String[] val = entry.getValue();
			if(val != null && val.length > 0 && val[0] != null){
				return val;
			}
		}
	}
	return new String[]{null};
}
	
	@Override
	public void updateNode(String nodeId, HashMap<String, Object> _props)
			throws Exception {
		
	}
	
	@Override
	public void writeContent(String nodeID, byte[] content, String mimetype,
			String encoding, String property) throws Exception {
	}


	@Override
	public void removeNode(String nodeID, String fromID, boolean recycle) throws Exception {
	}
	
}
