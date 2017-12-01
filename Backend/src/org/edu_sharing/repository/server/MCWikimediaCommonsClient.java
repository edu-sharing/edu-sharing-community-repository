package org.edu_sharing.repository.server;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
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
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MCWikimediaCommonsClient extends MCBaseClient {

	private Logger logger = Logger.getLogger(MCWikimediaCommonsClient.class);
	private String repositoryId = null;
	
	public MCWikimediaCommonsClient(String repoFile, HashMap authInfo) {
		
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfo(repoFile);
		this.repositoryId = appInfo.getAppId();
	}
	
	@Override
	public SearchResult searchRecommend(int startIdx, int nrOfResults)
			throws Throwable {

		SearchToken st = new SearchToken();
		st.setStartIDX(startIdx);
		st.setNrOfResults(nrOfResults);
				
		String searchword = ApplicationInfoList.getRepositoryInfoById(this.repositoryId).getRecommend_objects_query();
		if (searchword == null || searchword.trim().length() == 0) {
			searchword = "Mathematik";
		}
		
		HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> sd = 
				new SearchMetadataHelper().createSearchData(
						"searchword",
						"",
						new String[] {searchword});
		
		sd.keySet().iterator().next().setCriteriaboxid(MetadataSetQuery.DEFAULT_CRITERIABOXID);
		
		SearchCriterias sc = new SearchCriterias();		
		sc.setMetadataSetSearchData(sd);
		sc.setRepositoryId(this.repositoryId);
		
		st.setSearchCriterias(sc);
				
		return search(st);
	}
	
	@Override
	public SearchResult search(SearchToken searchToken) throws Throwable {
		
		HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> map = searchToken.getSearchCriterias().getMetadataSetSearchData();
		if(map != null){
			String searchWord = null;
			for(Map.Entry<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> entry:map.entrySet()){
				MetadataSetQuery mdsq = entry.getKey();
				
				if(mdsq != null && mdsq.getCriteriaboxid() != null){
					String criteriaboxid = mdsq.getCriteriaboxid();
								
					if(criteriaboxid.equals(MetadataSetQuery.DEFAULT_CRITERIABOXID)){
						String[] searchWords = getValue("searchword",entry.getValue());
						if(searchWords != null){
							
							if(searchWord == null) searchWord = "";
							
							for(String sw : searchWords){
								if(sw != null) searchWord = searchWord + " " + sw;
							}
							if(searchWord != null){
								searchWord = searchWord.trim();
							}
						}
					}
				}
			}
			
			if (searchWord != null) {
				
				URI uri1 = new URIBuilder()
		        .setScheme("http")
		        .setHost("commons.wikimedia.org")
		        .setPath("/w/api.php")
		        .setParameter("action", "query")
		        .setParameter("list", "search")
		        .setParameter("format", "json")
		        .setParameter("srnamespace", "6")
		        .setParameter("srinfo", "totalhits")
		        .setParameter("srsearch", searchWord)
		        .setParameter("srlimit", Integer.toString(searchToken.getNrOfResults()))
		        .setParameter("sroffset", Integer.toString(searchToken.getStartIDX()))
		        .build();

				try {
					
					SearchResult sr = new SearchResult();
					sr.setSearchCriterias(searchToken.getSearchCriterias());
																	
					JSONObject root1 = 
							(JSONObject) new JSONParser().parse(
									new HttpQueryTool().query(uri1.toString(), null, null));

					JSONObject query1 = (JSONObject) root1.get("query");
					JSONObject si1 = (JSONObject) query1.get("searchinfo");
					
					sr.setStartIDX(searchToken.getStartIDX());
					sr.setNodeCount(Integer.parseInt(si1.get("totalhits").toString()));					
					
					JSONArray search1 = (JSONArray) query1.get("search");
					
					StringBuilder titles = new StringBuilder(); 
					
					for ( int i = 0, c = search1.size()
						; i < c
						; ++i ){
						
						JSONObject entry = (JSONObject) search1.get(i);

						titles.append(i > 0 ? "|" : "")
							  .append(entry.get("title").toString());						
					}
										
					URI uri2 = new URIBuilder()
			        .setScheme("http")
			        .setHost("commons.wikimedia.org")
			        .setPath("/w/api.php")
			        .setParameter("action", "query")
			        .setParameter("prop", "imageinfo")
			        .setParameter("format", "json")
			        .setParameter("iiprop", "timestamp|canonicaltitle|url|size|dimensions|mime|thumbmime|mediatype|metadata|commonmetadata|extmetadata")
			        .setParameter("iiurlwidth", "250")
			        .setParameter("iiurlheight", "250")
			        .setParameter("titles", titles.toString())
			        .build();

					JSONObject root2 = 
							(JSONObject) new JSONParser().parse(
									new HttpQueryTool().query(uri2.toString(), null, null));

					JSONObject query2 = (JSONObject) root2.get("query");
					
					JSONObject pages2 = (JSONObject) query2.get("pages");
					
					HashMap<String,HashMap<String,Object>> resultData = new HashMap<String,HashMap<String,Object>>();
					sr.setData(resultData);

					for (Object value : pages2.values()) {
						
						HashMap<String,Object> props = getPropsByJSON((JSONObject) value);
									
						if (props != null) {
							
							resultData.put(
									(String) props.get(CCConstants.SYS_PROP_NODE_UID), 
									props);
						}
					}
					
					return sr;
					
				} catch(Throwable e) {
					
					logger.error(e.getMessage(), e);
				}
					
			}
		}
		
		return null;
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
	public MCBaseClient getInstance(String _repositoryFile, HashMap<String, String> _authenticationInfo) {
		return new MCWikimediaCommonsClient(_repositoryFile, _authenticationInfo);
	}
	
	@Override
	public boolean hasPermissions(String nodeId, String[] permissions) throws Exception {
		if(permissions != null && permissions[0].equals(CCConstants.PERMISSION_CC_PUBLISH)){
			return true;
		}else{
			return false;
		}
	}
	
	public java.util.HashMap<String,Object> getProperties(String nodeId) throws Throwable {
		
		URI uri2 = new URIBuilder()
        .setScheme("http")
        .setHost("commons.wikimedia.org")
        .setPath("/w/api.php")
        .setParameter("action", "query")
        .setParameter("prop", "imageinfo")
        .setParameter("format", "json")
        .setParameter("iiprop", "timestamp|canonicaltitle|url|size|dimensions|mime|thumbmime|mediatype|metadata|commonmetadata|extmetadata")
        .setParameter("iiurlwidth", "250")
        .setParameter("iiurlheight", "250")
        .setParameter("pageids", nodeId)
        .build();

		JSONObject root2 = 
				(JSONObject) new JSONParser().parse(
						new HttpQueryTool().query(uri2.toString(), null, null));

		JSONObject query2 = (JSONObject) root2.get("query");
		
		JSONObject pages2 = (JSONObject) query2.get("pages");
		
		JSONObject jo = (JSONObject) pages2.get(nodeId);
		
		return getPropsByJSON(jo);
	};
	
	
	@Override
	public void addAspect(String nodeId, String aspect) throws Exception {
	}
	
	@Override
	public void createAssociation(String fromID, String toID, String association) {
	} 
	
	@Override
	public String createNode(String parentID, String nodeTypeString, String childAssociation, HashMap<String, Object> _props) throws Exception {
		return null;
	}
	
	@Override
	public String createNode(String parentID, String nodeTypeString, HashMap<String, Object> _props) throws Exception {
		return null;
	}
	
	@Override
	public void createVersion(String nodeId, HashMap _properties) throws Exception {
	}
	
	@Override
	public List<String> getAssociationNodeIds(String nodeID, String association) throws Exception {
		return null;
	}
	
	public java.util.HashMap<String,Object> getChild(String parentId, String type, String property, String value) throws Throwable {
		return null;
	};
	
	@Override
	public HashMap<String, HashMap<String, Object>> getChildrenByType(String nodeId, String type) throws Exception {
		return null;
	}
	
	@Override
	public String getDetailsHtmlSnippet(String nodeId) throws Exception {
		return null;
	}
	
	public String getHomeFolderID(String username) throws Exception {return null;};
	
	@Override
	public MCBaseClient getInstance(HashMap<String, String> _authenticationInfo) {
		return null;
	}
	
	@Override
	public HashMap<String, String> getUserInfo(String userName) throws Exception {
		return null;
	}
	
	@Override
	public boolean hasContent(String nodeId, String contentProp) throws Exception {
		return false;
	}
	
	@Override
	public void removeAssociation(String fromID, String toID, String association) throws Exception {
	}
	
	@Override
	public void removeNode(String nodeID, String fromID) throws Exception {
	}
	
	@Override
	public void updateNode(String nodeId, HashMap<String, Object> _props) throws Exception {
	}
	
	@Override
	public void writeContent(String nodeID, byte[] content, String mimetype, String encoding, String property) throws Exception {
	}
		
	private HashMap<String,Object> getPropsByJSON(JSONObject jo){
				
		if (jo == null) {
			return null;
		}
		
		JSONArray iia = (JSONArray) jo.get("imageinfo");
		if (iia == null || iia.size() == 0) {
			return null;
		}

		JSONObject ii = (JSONObject) iia.get(0);
				
		HashMap<String,Object> props = new HashMap<String,Object>();
		
		String uid = jo.get("pageid").toString(); 
		
		props.put(
				CCConstants.SYS_PROP_NODE_UID,
				uid);

		props.put(
				CCConstants.REPOSITORY_ID, 
				this.repositoryId );
		
		props.put(
				CCConstants.CM_PROP_METADATASET_EDU_METADATASET, 
				CCConstants.metadatasetdefault_id );
		
		props.put(
				CCConstants.NODETYPE, 
				CCConstants.CCM_TYPE_IO);
								
		props.put(
				CCConstants.CONTENTURL, 
				getRedirectServletLink(repositoryId, uid));
									
		props.put(
				CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE,
				"image");
								
		if (ii.containsKey("mime")) {
			props.put(
					CCConstants.LOM_PROP_TECHNICAL_FORMAT,
					ii.get("mime").toString());
		}
		
		if (ii.containsKey("url")) {
			props.put(
					CCConstants.LOM_PROP_TECHNICAL_LOCATION,
					ii.get("url").toString());
		}
		
		if (ii.containsKey("thumburl")) {
			props.put(
					CCConstants.CM_ASSOC_THUMBNAILS, 
					ii.get("thumburl").toString());
		}
		
		JSONObject emd = (JSONObject) ii.get("extmetadata");
		if (emd != null) {
			
			JSONObject on = (JSONObject) emd.get("ObjectName");
			if (on != null && on.containsKey("value")) {
				props.put(
						CCConstants.LOM_PROP_GENERAL_TITLE, 
						on.get("value").toString());
			}
			
			JSONObject id = (JSONObject) emd.get("ImageDescription");
			if (id != null && id.containsKey("value")) {
				String desc = id.get("value").toString();
				
				props.put(
						CCConstants.LOM_PROP_GENERAL_DESCRIPTION,
						desc.startsWith("<") ? desc.substring(desc.indexOf(">")+1 , desc.lastIndexOf("<")) : desc);
			}
			
			JSONObject at = (JSONObject) emd.get("Artist");
			if (at != null && at.containsKey("value")) {
				String artist = at.get("value").toString(); 
				
				props.put(
						CCConstants.CM_PROP_C_CREATOR,
						artist.startsWith("<") ? artist.substring(artist.indexOf(">") + 1, artist.lastIndexOf("<")) : artist);
			}
			
			JSONObject dt = (JSONObject) emd.get("DateTime");
			if (dt != null && dt.containsKey("value")) {
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
				try {
		
					
					props.put(
							CCConstants.CM_PROP_C_MODIFIED,
							new DateTool().formatDate(
									sdf.parse(dt.get("value").toString()).getTime()));
					
				} catch (ParseException e) {			
					logger.error(e.getMessage(), e);
				}
			}
	
			JSONObject li = (JSONObject) emd.get("License");
			if (li != null && li.containsKey("value")) {

				String license = li.get("value").toString().replace("-",  "_").toUpperCase();
			
				String[] eduLicenses = new String[]{
						CCConstants.COMMON_LICENSE_CC_BY_NC_ND,
						CCConstants.COMMON_LICENSE_CC_BY_NC_SA,
						CCConstants.COMMON_LICENSE_CC_BY_NC,
						CCConstants.COMMON_LICENSE_CC_BY_ND,
						CCConstants.COMMON_LICENSE_CC_BY_SA,
						CCConstants.COMMON_LICENSE_CC_BY,								
				};
				
				for (String eduLicense : eduLicenses) {
					
					if (license.startsWith(eduLicense)) {
						
						props.put(
								CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, 
								eduLicense);
						
						break;
					}
				}
			}
		}
		
		return props;
	}

	@Override
	public void removeNode(String nodeID, String fromID, boolean recycle) throws Exception {	
	}	
}

