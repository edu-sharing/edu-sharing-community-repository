package org.edu_sharing.metadataset.v2.valuespace_reader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class OpenSALTReader extends ValuespaceReader{
    private static final String ASSOCIATION_IS_CHILD_OF = "isChildOf";
    private static final String ASSOCIATION_PRECEDES = "precedes";
    private String baseUrl;
    private String uuid;
    private static Logger logger = Logger.getLogger(OpenSALTReader.class);

    public OpenSALTReader(String valuespaceUrl) {
        super(valuespaceUrl);
        // e.g. http://localhost:3000/uri/8a2a94f0-36bd-11e9-bdc4-0242ac1a0003
        Matcher matched = matches("(https?:\\/\\/.*\\/)uri\\/(.*)");
        if(matched.matches()){
            this.baseUrl=matched.group(1);
            this.uuid=matched.group(2);
            logger.info("matched openSALT at "+baseUrl+" with uuid "+uuid);
        }
    }

    public List<MetadataKey> getValuespace(String locale) throws Exception{
        List<MetadataKey> result=new ArrayList<>();
        JSONObject list = getApi("CFPackages", uuid);
        JSONArray array = list.getJSONArray("CFItems");
        JSONArray associations = list.getJSONArray("CFAssociations");
        for(int i=0;i<array.length();i++){
            JSONObject entry=array.getJSONObject(i);

            String key = entry.getString("identifier");
            String caption = entry.getString("fullStatement");
            String parentId = getParentId(key,uuid,associations);

            MetadataKey metadataKey=new MetadataKey();
            metadataKey.setKey(key);
            metadataKey.setCaption(caption);
            metadataKey.setParent(parentId);
            metadataKey.setPreceds(getAssocs(key,ASSOCIATION_PRECEDES,associations));
            result.add(metadataKey);
        }
        return result;
    }
    private List<String> getAssocs(String key, String assocName, JSONArray associations) throws IOException, JSONException {
        List<String> assocs=new ArrayList<>();
        for(int i=0;i<associations.length();i++){
            if(associations.getJSONObject(i).getJSONObject("originNodeURI").getString("identifier").equals(key) && associations.getJSONObject(i).getString("associationType").equals(assocName)){
                String assoc = associations.getJSONObject(i).getJSONObject("destinationNodeURI").getString("identifier");
                if(!assoc.equals(key))
                    assocs.add(assoc);
            }
        }
        return assocs;
    }

    private String getParentId(String key, String mainParent, JSONArray associations) throws IOException, JSONException {
        for(String assoc : getAssocs(key,ASSOCIATION_IS_CHILD_OF,associations)){
            if(!assoc.equals(mainParent))
                return assoc;
        }
        return null;
    }

    private JSONObject getApi(String method, String uuid) throws IOException, JSONException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpUriRequest request=new HttpGet(getApiUrl(method,uuid));
        CloseableHttpResponse result = httpclient.execute(request);
        String data=StreamUtils.copyToString(result.getEntity().getContent(), StandardCharsets.UTF_8);
        result.close();
        return new JSONObject(data);
    }
    private String getApiUrl(String method, String uuid) {
        return baseUrl+"/ims/case/v1p0/"+ URLEncoder.encode(method)+"/"+ URLEncoder.encode(uuid);
    }

    @Override
    protected boolean supportsUrl() {
        return baseUrl != null;
    }
}
