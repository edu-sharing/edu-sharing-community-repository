package org.edu_sharing.metadataset.v2;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class OpenSALTReader {
    private static final String ASSOCIATION_IS_CHILD_OF = "isChildOf";
    private final String baseUrl;
    private static Logger logger = Logger.getLogger(OpenSALTReader.class);

    public OpenSALTReader(String baseUrl) {
        this.baseUrl=baseUrl;
    }

    public List<MetadataKey> getValuespace(String uuid) throws Exception{
        List<MetadataKey> result=new ArrayList<>();
        JSONObject list = getApi("CFPackages", uuid);
        JSONArray array = list.getJSONArray("CFItems");
        for(int i=0;i<array.length();i++){
            JSONObject entry=array.getJSONObject(i);

            String key = entry.getString("identifier");
            String caption = entry.getString("fullStatement");
            String parentId = getParentId(key,uuid);

            MetadataKey metadataKey=new MetadataKey();
            metadataKey.setKey(key);
            metadataKey.setCaption(caption);
            metadataKey.setParent(parentId);
            result.add(metadataKey);
        }
        return result;
    }

    private String getParentId(String key,String mainParent) throws IOException, JSONException {
        JSONObject entryAssocs=getApi("CFItemAssociations",key);
        JSONArray entryassocsArray = entryAssocs.getJSONArray("CFAssociations");
        for(int i=0;i<entryassocsArray.length();i++){
            if(entryassocsArray.getJSONObject(i).getString("associationType").equals(ASSOCIATION_IS_CHILD_OF)){
                String parent = entryassocsArray.getJSONObject(i).getJSONObject("destinationNodeURI").getString("identifier");
                if(!parent.equals(mainParent) && !parent.equals(key))
                    return parent;
            }
        }
        return null;
    }

    private JSONObject getApi(String method, String uuid) throws IOException, JSONException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpUriRequest request=new HttpGet(getApiUrl(method,uuid));
        CloseableHttpResponse result = httpclient.execute(request);
        String data=StreamUtils.copyToString(result.getEntity().getContent(),Charset.forName("UTF-8"));
        result.close();
        return new JSONObject(data);
    }
    private String getApiUrl(String method, String uuid) {
        return baseUrl+"/ims/case/v1p0/"+ URLEncoder.encode(method)+"/"+ URLEncoder.encode(uuid);
    }
}
