package org.edu_sharing.service.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchServiceDDBImpl extends SearchServiceAdapter{
	
	public static final String DDB_API = "https://api.deutsche-digitale-bibliothek.de";

	Logger logger = Logger.getLogger(SearchServiceDDBImpl.class);
	
	String repositoryId = null;

	String APIKey = null;

	private ApplicationInfo appInfo;
			
	
	public SearchServiceDDBImpl(String appId) {
		appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		this.APIKey = appInfo.getApiKey(); 

	}

	public static HttpsURLConnection openDDBUrl(URL url) throws KeyManagementException, IOException, NoSuchAlgorithmException{
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		
		// TODO!
		TrustManager[] trustAllCerts = new TrustManager[]{
			    new X509TrustManager() {
			        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			            return null;
			        }
			        public void checkClientTrusted(
			            java.security.cert.X509Certificate[] certs, String authType) {
			        }
			        public void checkServerTrusted(
			            java.security.cert.X509Certificate[] certs, String authType) {
			        }
			    }
			};
		SSLContext sc = SSLContext.getInstance("SSL");
	    sc.init(null, trustAllCerts, new java.security.SecureRandom());
		connection.setSSLSocketFactory(sc.getSocketFactory());
		connection.setHostnameVerifier(new HostnameVerifier() {
		    public boolean verify(String hostname, SSLSession session) {
		      return true;
		    }
		  });
		return connection;
	}
	public  SearchResultNodeRef searchDDB(String repositoryId,String apiKey,String path) throws Exception{
		//String lang=new AuthenticationToolAPI().getCurrentLocale().split("_")[0];

		String jsonString = "";
		
		String urlAsStr = DDB_API+path;
		jsonString = query(urlAsStr);
		JSONObject jo = new JSONObject(jsonString);
    	
		Integer nrOfResult = (Integer)jo.get("numberOfResults");
		JSONArray resultsArr = (JSONArray)jo.get("results");
		JSONObject results = (JSONObject)resultsArr.get(0);
		JSONArray docs = (JSONArray)results.get("docs");

		
		SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
		List<NodeRef> data=new ArrayList<>();
		
		HashMap<String,HashMap<String,Object>> result = new HashMap<String, HashMap<String,Object>>();
		for(int i = 0; i < docs.length(); i++){
			JSONObject doc = (JSONObject)docs.get(i);
			String id =  this.getNodeId(doc);
			org.edu_sharing.service.model.NodeRef ref = new org.edu_sharing.service.model.NodeRefImpl(
					repositoryId, 
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
					id);
			data.add(ref);

		}
		// prefetch properties parallel for better performance
		/*
		// does currently not result in better performance since api gets slower with parallel requests
		NodeServiceDDBImpl nodeServiceDDB = new NodeServiceDDBImpl(appInfo.getAppId());
		data = data.parallelStream().map((item) -> {
			try {
				long time = System.currentTimeMillis();
				item.setProperties(nodeServiceDDB.getProperties(null, null, item.getNodeId()));
				logger.info("Time ddb: " + (System.currentTimeMillis() - time));
			} catch (Throwable throwable) {
				logger.warn("DDB properties fetch failed", throwable);
			}
			return item;
		}).collect(Collectors.toList());
		 */
		searchResultNodeRef.setData(data);

		/*

		
			HashMap<String,Object> properties=new HashMap<>();
			properties.put(CCConstants.SYS_PROP_NODE_UID,map.getNamedItem("id").getNodeValue());
			properties.put(CCConstants.LOM_PROP_GENERAL_TITLE,map.getNamedItem("title").getNodeValue());
			
			properties.put(CCConstants.LOM_PROP_GENERAL_KEYWORD,map.getNamedItem("tags").getNodeValue().replace(" ",CCConstants.MULTIVALUE_SEPARATOR));

			properties.put(CCConstants.CM_ASSOC_THUMBNAILS, map.getNamedItem("image").getNodeValue());
			properties.put(CCConstants.CONTENTURL,map.getNamedItem("url").getNodeValue());
			properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "application/xhtml+xml");
			properties.put(CCConstants.CCM_PROP_IO_WWWURL,map.getNamedItem("url").getNodeValue());
			properties.put(CCConstants.NODETYPE, CCConstants.CCM_TYPE_IO);
			properties.put(CCConstants.CM_PROP_C_CREATOR,map.getNamedItem("author").getNodeValue());

			properties.put(CCConstants.REPOSITORY_ID, "LEARNINGAPPS" );
			properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"LearningApps");
			properties.put(CCConstants.CM_PROP_C_CREATOR,map.getNamedItem("author").getNodeValue());
			properties.put(CCConstants.NODECREATOR_FIRSTNAME,map.getNamedItem("author").getNodeValue());
			properties.put(CCConstants.NODEMODIFIER_FIRSTNAME,map.getNamedItem("author").getNodeValue());
			
			String createdate = map.getNamedItem("created").getNodeValue();

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
			LocalDateTime date  = LocalDateTime.parse(createdate, formatter);
			DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
			properties.put(CCConstants.CM_PROP_C_MODIFIED,date.format(formatter2));
			  
		}*/
		return searchResultNodeRef;

	}
	
	private String query(String urlAsStr) throws Exception {
		URL url = new URL(urlAsStr);
		HttpsURLConnection connection = openDDBUrl(url);
		connection.connect();		

        InputStreamReader isr = new InputStreamReader(connection.getInputStream(), "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        isr.close();
        connection.disconnect();
        return sb.toString();
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

	private String getNodeId(JSONObject doc){
		String id = null;
		try {
			id = (String)doc.get("id");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return id;
	}


	public List<? extends  Suggestion> getSuggestions(MetadataSet mds, String queryId, String parameterId, String value) {
		
		List<Suggestion> result = new ArrayList<Suggestion>();
		
		List<String> facets = mds.findQuery(queryId, MetadataReader.QUERY_SYNTAX_LUCENE).findParameterByName(parameterId).getFacets();
		//String url = getUrl("/search",parameterId +":("+value+")",facets, 0, 0);
		String url = getUrl("/search","*",facets, 0, 0);
		System.out.println("url:" + url);
		
		try {
			String json = this.query(url);
			System.out.println(json);
			JSONObject jo = new JSONObject(json);
	    	
			JSONArray resultsArr = (JSONArray)jo.get("facets");
			
			for(int i = 0; i < resultsArr.length(); i++) {
				JSONObject facetObj = (JSONObject)resultsArr.get(i);
				String field = facetObj.getString("field");
				int numberOfFacets = facetObj.getInt("numberOfFacets");
				if(facets.contains(field) && numberOfFacets > 0) {
					
					JSONArray facetValues = facetObj.getJSONArray("facetValues");
					for(int j = 0; j < facetValues.length(); j++) {
						JSONObject facetteVal = (JSONObject)facetValues.get(j);
						//int count = facetteVal.getInt("count");
						String val = facetteVal.getString("value");

						if(val.contains(value)) {
							Suggestion dto = new Suggestion();
							dto.setKey(val);
							dto.setDisplayString(val);

							result.add(dto);
						}
					}
					
					
				}
			}
	
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return result;
	}
	
	public String getUrl(String basePath, String query, List<String> facets, int offset, int rows){
		String url = DDB_API + basePath;
		
		try {
			url += "?oauth_consumer_key=" + URLEncoder.encode(APIKey, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		url += "&query=" + org.springframework.extensions.surf.util.URLEncoder.encodeUriComponent(query);
		if(facets != null && facets.size() > 0) {
			url += "&facet=";
			int i = 0;
			for(String facet :  facets) {
				if(i == 0) {
					url += facet;
				}else {
					url += "," +facet;
				}
				
			}
		}
		url += "&offset="+offset;
		url += "&rows="+rows;
		
		return url;
	}
	
	@Override
	public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
									  SearchToken searchToken) throws Throwable {
		

		if(!MetadataSet.DEFAULT_CLIENT_QUERY.equals(query)){
			throw new Exception("Only ngsearch query is supported for this repository type, requested "+query);
		}
		
        /*
         * 		String url = baseUrl+"/search";
        		url = url + "?oauth_consumer_key=" + URLEncoder.encode(apiKey, "UTF-8");
        		url = url + "&"+queryParams;
        		url = url + "&offset="+offset;
        		url = url + "&rows=" +rows;
         * 		
         */

		String[] searchWordCriteria=criterias.get(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA);

		List<String> extSearch = new ArrayList<String>();
		
 		String searchWord = "";
 		if(searchWordCriteria!=null && searchWordCriteria.length>0) {
 			searchWord = searchWordCriteria[0];
 		}
		if (searchWord.equals("*") ){
			searchWord="";
		}

		boolean retval;

 		if(criterias.containsKey("title")) {
 			String ddbTitle =criterias.get("title")[0];
 			if (!ddbTitle.equals("") ){
 				extSearch.add("title:("+ddbTitle+")");
 			}
 		}

 		if(criterias.containsKey("place")) {
 			String ddbPlace =criterias.get("place")[0];
 			if (!ddbPlace.equals("") ){
 				extSearch.add("place:("+ddbPlace+")");
 			}
 		}

 		if(criterias.containsKey("affiliate")) {
 			String ddbPerson =criterias.get("affiliate")[0];
 			if (!ddbPerson.equals("") ){
 				extSearch.add("affiliate:("+ddbPerson+")");
 			}
 		}
		
		HttpsURLConnection connection=null;

/*		if(searchToken.getFrom()%searchToken.getMaxResult()!=0)
			throw new Exception("Pixabay only supports offsets which are dividable by the maxItems count");
	*/
		String ext = "";
		for (String s : extSearch) {
		    if (!s.equals("") && !extSearch.get(extSearch.size() - 1).equals(s)){
		     ext = ext+s +" AND ";	
		    }else{
			     ext = ext+s;	
		    }
		}
		

		try {


			String oauth  = "/search?oauth_consumer_key=" + URLEncoder.encode(this.APIKey, "UTF-8");
			String offset = "&offset="+searchToken.getFrom();
			String rows = "&rows="+searchToken.getMaxResult();

			String uri=oauth+"&query="+org.springframework.extensions.surf.util.URLEncoder.encodeUriComponent(searchWord+" "+ext)+offset+rows;

			searchToken.setQueryString(uri);

			return searchDDB(repositoryId,APIKey,uri);

		}
		catch(IOException e){
			InputStream is=connection.getErrorStream();
			StringBuilder responseStrBuilder = new StringBuilder();
			String line;
			BufferedReader bR = new BufferedReader(  new InputStreamReader(is));
			while((line =  bR.readLine()) != null){
			    responseStrBuilder.append(line);
			}
			is.close();
			throw new Exception("Error from DDB: "+connection.getResponseCode()+" "+responseStrBuilder.toString());
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}

	}
	
	public static void main(String[] args) {
		try {
			String searchWord = "*";
			String extendedSearch = "place:(Frankfurt)";
			String oauthKey = "nVyX1bwLOAEpMVrzfEIf3xth5eTtVOaqZeeUUcUEQDNa4Oigs6y1438781244192";
			String path = "/search";
			path += "?oauth_consumer_key=" + URLEncoder.encode(oauthKey, "UTF-8");
			path += "&query=" + org.springframework.extensions.surf.util.URLEncoder.encodeUriComponent(searchWord+" " +extendedSearch);
			path += "&facet=place_fct";
			path += "&offset="+0;
			path += "&rows="+0;
			
			
			URL url=new URL(DDB_API+path);
			HttpsURLConnection connection = openDDBUrl(url);
			connection.connect();		
	
	        InputStreamReader isr = new InputStreamReader(connection.getInputStream(), "UTF-8");
	        BufferedReader br = new BufferedReader(isr);
	        StringBuilder sb = new StringBuilder();
	        String line;
	        while ((line = br.readLine()) != null) {
	            sb.append(line);
	        }
	        br.close();
	        isr.close();
	        connection.disconnect();
	        String jsonString = sb.toString();
	        System.out.println(jsonString);
			JSONObject jo = new JSONObject(jsonString);
	    	
			Integer nrOfResult = (Integer)jo.get("numberOfResults");
			JSONArray resultsArr = (JSONArray)jo.get("results");
			JSONObject results = (JSONObject)resultsArr.get(0);
			JSONArray docs = (JSONArray)results.get("docs");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	
}
