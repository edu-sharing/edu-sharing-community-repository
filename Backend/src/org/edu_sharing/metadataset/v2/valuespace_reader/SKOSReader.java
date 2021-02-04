package org.edu_sharing.metadataset.v2.valuespace_reader;

import com.google.gson.JsonObject;
import org.apache.http.client.config.RequestConfig;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class SKOSReader extends ValuespaceReader{
    private static final String ASSOCIATION_IS_CHILD_OF = "isChildOf";
    private String url;
    private static Logger logger = Logger.getLogger(SKOSReader.class);

    public SKOSReader(String valuespaceUrl) {
        super(valuespaceUrl);
        // e.g. http://localhost:8000/api/v1/curricula/metadatasets
        Matcher matched = matches("(https?:\\/\\/.*\\/)w3id\\.org\\/.*\\.json");
        if(matched.matches()){
            url = valuespaceUrl;
            logger.info("matched SKOS at "+matched.group(1));
        }
    }

    @Override
    public List<MetadataKey> getValuespace(String locale) throws Exception {
        return getValuespace(fetch(), null, locale);
    }

    public List<MetadataKey> getValuespace(JSONArray list, MetadataKey parent, String locale) throws Exception {
        List<MetadataKey> result = new ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            MetadataKey converted = convertEntry(entry, locale);
            if(parent != null){
                converted.setParent(parent.getKey());
            }
            result.add(converted);
            if(entry.has("narrower")){
                List<MetadataKey> subList = getValuespace(entry.getJSONArray("narrower"), converted, locale);
                result.addAll(subList);
            }
        }
        return result;
    }

    private MetadataKey convertEntry(JSONObject entry, String locale) throws JSONException {
        MetadataKey key = new MetadataKey();
        key.setKey(entry.getString("id"));
        String de = entry.getJSONObject("prefLabel").getString("de");
        key.setCaption(de);
        key.setLocale("de");
        if("en_US".equals(locale)) {
            try {
                key.setCaption(entry.getJSONObject("prefLabel").getString("en"));
                key.setLocale("en");
            }catch(JSONException ignored) { }
        }
        // @TODO handle tree structures
        //key.setParent(entry.isNull("parent_id") ? null : entry.getString("parent_id"));
        return key;
    }

    private JSONArray fetch() throws IOException, JSONException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom().
                setConnectTimeout(30000).
                build();
        HttpGet request=new HttpGet(url);
        request.setConfig(requestConfig);
        CloseableHttpResponse result = httpclient.execute(request);
        String data=StreamUtils.copyToString(result.getEntity().getContent(), StandardCharsets.UTF_8);
        result.close();
        return new JSONObject(data).getJSONArray("hasTopConcept");
    }

    @Override
    protected boolean supportsUrl() {
        return url != null;
    }
}
