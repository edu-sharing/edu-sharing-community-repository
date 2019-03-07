package org.edu_sharing.service.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeServicePixabayImpl;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.URLEncoder;

import com.google.common.collect.MapMaker;

public class SearchServicePixabayImpl extends SearchServiceAdapter{
	
	private static final String PIXABAY_API = "https://pixabay.com/api/";

	Logger logger = Logger.getLogger(SearchServicePixabayImpl.class);
	
	String repositoryId = null;

	String APIKey = null;
			
	private static ConcurrentMap<String,String> searchCache=new MapMaker().expiration(30, TimeUnit.MINUTES).makeMap();
	public SearchServicePixabayImpl(String appId) {
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		APIKey = appInfo.getApiKey(); 

	}
	public static HttpsURLConnection openPixabayUrl(URL url) throws KeyManagementException, IOException, NoSuchAlgorithmException{
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
	public static SearchResultNodeRef searchPixabay(String repositoryId,String apiKey,String path) throws Exception{
		String lang=new AuthenticationToolAPI().getCurrentLocale().split("_")[0];
		String url=PIXABAY_API+"?key="+URLEncoder.encodeUriComponent(apiKey)+"&lang="+lang+path;
		String jsonString;
		if(searchCache.containsKey(url)){
			jsonString=searchCache.get(url);
		}
		else{
			URL urlURL=new URL(url);
			HttpsURLConnection connection = openPixabayUrl(urlURL);
			connection.connect();
			InputStream is=connection.getInputStream();
			StringBuilder responseStrBuilder = new StringBuilder();
			String line;
			BufferedReader bR = new BufferedReader(  new InputStreamReader(is));
			while((line =  bR.readLine()) != null){
			    responseStrBuilder.append(line);
			}
			is.close();
			jsonString=responseStrBuilder.toString();
			searchCache.put(url,jsonString);
		}
		JSONObject result= new JSONObject(jsonString);       
		JSONArray array=result.getJSONArray("hits");
		SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
		List<NodeRef> data=new ArrayList<>();
		searchResultNodeRef.setNodeCount(result.getInt("total"));
		searchResultNodeRef.setData(data);
		for(int i=0;i<array.length();i++){
			JSONObject json=array.getJSONObject(i);
			
			HashMap<String,Object> properties=new HashMap<>();
			properties.put(CCConstants.SYS_PROP_NODE_UID,json.getString("id"));
			properties.put(CCConstants.CM_PROP_C_MODIFIED,System.currentTimeMillis());
			properties.put(CCConstants.LOM_PROP_GENERAL_TITLE,json.getString("tags"));
			properties.put(CCConstants.LOM_PROP_GENERAL_KEYWORD,json.getString("tags").replace(", ",CCConstants.MULTIVALUE_SEPARATOR));
			String[] split=json.getString("webformatURL").split("\\.");;
			String extension=split[split.length-1].toLowerCase();
			if(extension.equals("jpg"))
				properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "image/jpeg");
			if(extension.equals("png"))
				properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "image/png");
			if(json.getString("type").equals("film"))
				properties.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, "video/*");
			properties.put(CCConstants.ALFRESCO_MIMETYPE,properties.get(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
			properties.put(CCConstants.CM_NAME,json.getString("tags")+"."+extension);
			properties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,CCConstants.COMMON_LICENSE_CC_ZERO);
			properties.put(CCConstants.CM_PROP_C_CREATOR,json.getString("user"));
			properties.put(CCConstants.NODECREATOR_FIRSTNAME,json.getString("user"));
			properties.put(CCConstants.NODEMODIFIER_FIRSTNAME,json.getString("user"));
			String author=VCardTool.nameToVCard(json.getString("user"));
			properties.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR,author);
			properties.put(CCConstants.VIRT_PROP_USAGECOUNT,json.getInt("downloads"));
			properties.put(CCConstants.CCM_PROP_IO_WIDTH,json.getString("imageWidth"));
			properties.put(CCConstants.CCM_PROP_IO_HEIGHT,json.getString("imageHeight"));
			properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"pixabay");
			//properties.put(CCConstants.CONTENTURL,json.getString("pageURL"));
			properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION,json.getString("pageURL"));
			properties.put(CCConstants.CONTENTURL,URLTool.getRedirectServletLink(repositoryId, json.getString("id")));
			properties.put(CCConstants.VIRT_PROP_PERMALINK,json.getString("pageURL"));
			//properties.put(CCConstants.CM_ASSOC_THUMBNAILS, json.getString("previewURL"));
			properties.put(CCConstants.CM_ASSOC_THUMBNAILS,json.getString("webformatURL").replace("_640", "_340"));
			properties.put(CCConstants.DOWNLOADURL,json.getString("webformatURL").replace("_640", "_960"));
			org.edu_sharing.service.model.NodeRef ref = new org.edu_sharing.service.model.NodeRefImpl(repositoryId, 
					Constants.storeRef.getProtocol(),
					Constants.storeRef.getIdentifier(),properties);
			data.add(ref);
			NodeServicePixabayImpl.updateCache(json.getString("id"), properties);
		}
		return searchResultNodeRef;

	}
	
	@Override
	public SearchResultNodeRef searchV2(MetadataSetV2 mds, String query, Map<String, String[]> criterias,
			SearchToken searchToken) throws Throwable {
				
		if(!MetadataSetV2.DEFAULT_CLIENT_QUERY.equals(query)){
			throw new Exception("Only ngsearch query is supported for this repository type, requested "+query);
		}
		
		boolean editorsChoice=false;
		String imageType="all";
		String orientation="all";
		String[] searchWordCriteria=criterias.get(MetadataSetV2.DEFAULT_CLIENT_QUERY_CRITERIA);
		if(criterias.containsKey("editors_choice"))
			editorsChoice=criterias.get("editors_choice")[0].equals("true");
		if(criterias.containsKey("image_type"))
			imageType = criterias.get("image_type")[0];
		if(criterias.containsKey("orientation"))
			orientation = criterias.get("orientation")[0];
		
		if(searchWordCriteria == null){
			searchWordCriteria = new String[] {"*"};
		}
		String searchWord = searchWordCriteria[0];
		if(searchToken.getFrom()%searchToken.getMaxResult()!=0)
			throw new Exception("Pixabay only supports offsets which are dividable by the maxItems count");
		try {
			
			int page=1 + searchToken.getFrom()/searchToken.getMaxResult();
			String uri="&q="+URLEncoder.encodeUriComponent(searchWord)+
					"&editors_choice="+editorsChoice+
					"&image_type="+imageType+
					"&orientation="+orientation+
					"&safesearch=true"+
					"&per_page="+searchToken.getMaxResult()+
					"&page="+page;
			searchToken.setQueryString(uri);
			return searchPixabay(repositoryId,APIKey,uri);
			
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}

	}

}
