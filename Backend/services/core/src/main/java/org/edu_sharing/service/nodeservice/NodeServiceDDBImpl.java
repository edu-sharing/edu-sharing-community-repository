package org.edu_sharing.service.nodeservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServiceDDBImpl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NodeServiceDDBImpl extends NodeServiceAdapterCached{
	Logger logger = Logger.getLogger(NodeServiceDDBImpl.class);

	private final String APIKey;
	
	public NodeServiceDDBImpl(String appId) {
		super(appId);
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.APIKey = appInfo.getApiKey();
	}

	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		HashMap<String, Object> props = super.getProperties(storeProtocol, storeId, nodeId);
		if(props != null) {
			return props;
		}

		HashMap<String,Object> properties = new  HashMap<String,Object>();
		properties.put(CCConstants.SYS_PROP_NODE_UID,nodeId);
		String url = "https://www.deutsche-digitale-bibliothek.de/item/"+nodeId;
		properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, url);
		properties.put(CCConstants.CCM_PROP_IO_WWWURL, url);
		//properties.put(CCConstants.CONTENTURL,URLTool.getRedirectServletLink(this.repositoryId, nodeId));
		properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE,"ddb");
		try{
			// fetch binary info
			String all = SearchServiceDDBImpl.httpGet(SearchServiceDDBImpl.DDB_API+"/items/"+nodeId+"/aip?oauth_consumer_key=" + URLEncoder.encode(this.APIKey, "UTF-8"), null);
			for(int i = 1; i < 10; i++) {
				// ULTRA DIRTY HACK to prevent json from failing with duplicate keys!
				all = all.replaceFirst("\"Concept\":", "\"Concept"+i+"\":")
						.replaceFirst("\"Agent\":", "\"Agent"+i+"\":")
						.replaceFirst("\"rights\":", "\"rights"+i+"\":")
						.replaceFirst("\"subject\":", "\"subject"+i+"\":")
						.replaceFirst("\"Event\":", "\"subject"+i+"\":")
						.replaceFirst("\"language\":", "\"language"+i+"\":");
			}
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
				contenturl = SearchServiceDDBImpl.DDB_API+contenturl+"?oauth_consumer_key=" + URLEncoder.encode(this.APIKey, "UTF-8");

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
					String creator=getLanguageString(meta, "creator");
					properties.put(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR,
							VCardTool.nameToVCard(creator));
					properties.put(CCConstants.CM_PROP_C_CREATOR,creator);
					properties.put(CCConstants.NODECREATOR_FIRSTNAME,creator);
					properties.put(CCConstants.NODEMODIFIER_FIRSTNAME,creator);
				}catch(Throwable t) {}
				try {
					String name=getLanguageString(meta, "title");
					properties.put(CCConstants.CM_NAME, name);
					properties.put(CCConstants.LOM_PROP_GENERAL_TITLE, name);
				}catch(Throwable t) {}
			}catch(Throwable t) {}


			String imageUrl = null;

			try {

				JSONObject joPreview = (JSONObject)allJson.get("preview");
				if(joPreview.has("thumbnail") && joPreview.get("thumbnail") != null && joPreview.get("thumbnail") instanceof JSONObject) {
					JSONObject joThumbnail = (JSONObject)joPreview.getJSONObject("thumbnail");

					String previewId = (String)joThumbnail.get("@href");


					String thumbUrl = "https://iiif.deutsche-digitale-bibliothek.de/image/2/" + previewId + "/info.json";
					String thumbResult = SearchServiceDDBImpl.httpGet(thumbUrl, null);

					JSONObject jo = new JSONObject(thumbResult);
					JSONArray ja = (JSONArray)jo.get("sizes");



					JSONObject joSize = (JSONObject)ja.get(0);

					if(ja.length() > 3) {
						joSize = (JSONObject)ja.get(1);
					}

					Integer width = (Integer)joSize.get("width");
					Integer height = (Integer)joSize.get("height");

					imageUrl = "https://iiif.deutsche-digitale-bibliothek.de/image/2/" + previewId + "/full/!" + width + "," + height + "/0/default.jpg";
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			/*String previewUrl;
			try {
				previewUrl=DDB_API+allJson.getJSONObject("preview").getJSONObject("thumbnail").getString("@href")+"?oauth_consumer_key=" + URLEncoder.encode(this.APIKey, "UTF-8");
			}
			catch(Throwable t) {
				previewUrl=new MimeTypesV2(appInfo).getPreview(CCConstants.CCM_TYPE_IO, properties, null);
			}*/
			if(imageUrl != null) {
				properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, imageUrl);
			}
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

								if("license".equals(fid) && value!=null){
									if(value.contains("/by-sa")){
										properties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_SA);
									}
									if(value.contains("/by-nc-sa")){
										properties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.COMMON_LICENSE_CC_BY_NC_SA);
									}
									if(value.contains("/rv-fz")){
										properties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, CCConstants.LICENSE_COPYRIGHT_FREE);
									}
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
			e.printStackTrace();
		}

		updateCache(properties);
		return properties;
   }

	private String getLanguageString(JSONObject meta, String field) throws JSONException {
		try {
			return meta.getJSONArray(field).getJSONObject(0).getString("$");
		}catch (Throwable t1) {
			try {
				return meta.getJSONObject(field).getString("$");
			} catch (Throwable t2) {
				return meta.getString(field);
			}
		}
	}
	@Override
	public String getType(String nodeId) {
		// TODO Auto-generated method stub
		return CCConstants.CCM_TYPE_IO;
	}
	
	@Override
	public String[] getAspects(String storeProtocol, String storeId, String nodeId) {
		// TODO Auto-generated method stub
		return new String[] {};
	}

	@Override
	public InputStream getContent(String nodeId) throws Throwable {
		return null;
	}
}