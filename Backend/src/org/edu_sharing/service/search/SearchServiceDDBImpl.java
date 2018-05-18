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

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchServiceDDBImpl extends SearchServiceAdapter{
	
	private static final String DDB_API = "https://api.deutsche-digitale-bibliothek.de";

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
        jsonString = sb.toString();

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
					Constants.storeRef.getProtocol(),
					Constants.storeRef.getIdentifier(),
					id);
			data.add(ref);

		}
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
	
	
	public HashMap<String, Object> getProperties(String nodeId) throws Throwable {
		
		HashMap<String,Object> properties = new  HashMap<String,Object>();
		properties.put(CCConstants.SYS_PROP_NODE_UID,nodeId);
		String url = "https://www.deutsche-digitale-bibliothek.de/item/"+nodeId;
		properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, url);
		properties.put(CCConstants.CCM_PROP_IO_WWWURL, url);
        properties.put(CCConstants.CONTENTURL,URLTool.getRedirectServletLink(this.repositoryId, nodeId));
		properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"ddb");
		try{
			// fetch binary info
			String all = httpGet(DDB_API+"/items/"+nodeId+"/aip?oauth_consumer_key=" + URLEncoder.encode(this.APIKey, "UTF-8"), null);
			JSONObject allJson = new JSONObject(all);
			try {
				JSONArray binaries = (JSONArray)allJson.getJSONObject("binaries").getJSONArray("binary");			
				JSONObject binary = null;
				JSONObject binaryFallback = binaries.getJSONObject(0);
				
				for(int i = 0; i < binaries.length();i++){
					binary =  (JSONObject)binaries.get(0);						
					String path = (String)binary.get("@path");
					
					// prefer mvpr
					if(path.contains("mvpr")){
						break;
					}
				}
				
				if(binary == null){
					binary = binaryFallback;
				}
				String name  = (String)binary.get("@name");
				properties.put(CCConstants.CM_NAME, name);
				properties.put(CCConstants.LOM_PROP_GENERAL_TITLE, name);
				String contenturl  = (String)binary.get("@path");
				String mimetyp = (String)binary.get("@mimetype");
				contenturl = DDB_API+contenturl+"?oauth_consumer_key=" + URLEncoder.encode(this.APIKey, "UTF-8");
				
				//properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, contenturl);

				properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, mimetyp);
			}catch(Throwable t) {}
			try {
				JSONObject meta=allJson.getJSONObject("edm").getJSONObject("RDF").getJSONObject("ProvidedCHO");
				try {
				properties.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER,
						VCardTool.nameToVCard(meta.getString("publisher")));
				}catch(Throwable t) {}
				try {
					properties.put(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR,
							VCardTool.nameToVCard(meta.getString("creator")));
					properties.put(CCConstants.CM_PROP_C_CREATOR,meta.getString("creator"));
					properties.put(CCConstants.NODECREATOR_FIRSTNAME,meta.getString("creator"));
					properties.put(CCConstants.NODEMODIFIER_FIRSTNAME,meta.getString("creator"));
				}catch(Throwable t) {}
				try {
					String name=meta.getString("title");
					properties.put(CCConstants.CM_NAME, name);
					properties.put(CCConstants.LOM_PROP_GENERAL_TITLE, name);
				}catch(Throwable t) {}
			}catch(Throwable t) {}
			
			
			String previewUrl;
			try {
				previewUrl=DDB_API+allJson.getJSONObject("preview").getJSONObject("thumbnail").getString("@href")+"?oauth_consumer_key=" + URLEncoder.encode(this.APIKey, "UTF-8");
			}
			catch(Throwable t) {
				previewUrl=new MimeTypesV2(appInfo).getPreview(CCConstants.CCM_TYPE_IO, properties, null);
			}
			properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, previewUrl);	
			//for the gwt gui no persistent
			properties.put(CCConstants.CM_ASSOC_THUMBNAILS, previewUrl);
			
			JSONObject item = allJson.getJSONObject("view").getJSONObject("item");				
			try {
				if(item != null){
					JSONArray fields = (JSONArray)item.get("fields");
					
					for(int i = 0; i < fields.length();i++){
						JSONObject fieldsObj = (JSONObject)fields.get(i);
						String usage = (String)fieldsObj.get("@usage");
						if("index".equals(usage)){
							JSONArray entries = (JSONArray)fieldsObj.get("field");
							for(int f = 0; f < entries.length();f++){
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
							for(int f = 0; f < entries.length();f++){
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
				}
			}
			catch(Throwable t) {}
			try {
				JSONObject institution = (JSONObject)item.get("institution");
				if(institution != null){
					String instName = (String)institution.get("name");
					properties.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR+"FN", instName);
					
				}
			}catch(Throwable t) {
				
			}
		}
		catch (Exception e) {
			
		}
			
			
		return properties;
	}
	
	
	
	@Override
	public SearchResultNodeRef searchV2(MetadataSetV2 mds, String query, Map<String, String[]> criterias,
			SearchToken searchToken) throws Throwable {
		

		if(!MetadataSetV2.DEFAULT_CLIENT_QUERY.equals(query)){
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

		String[] searchWordCriteria=criterias.get(MetadataSetV2.DEFAULT_CLIENT_QUERY_CRITERIA);

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
	
}
