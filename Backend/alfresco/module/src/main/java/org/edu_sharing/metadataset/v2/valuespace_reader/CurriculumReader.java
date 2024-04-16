package org.edu_sharing.metadataset.v2.valuespace_reader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.ValuespaceData;
import org.edu_sharing.metadataset.v2.ValuespaceInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class CurriculumReader extends ValuespaceReader{
    private static final String ASSOCIATION_IS_CHILD_OF = "isChildOf";
    private String url;
    private static Logger logger = Logger.getLogger(CurriculumReader.class);

    public CurriculumReader(ValuespaceInfo info) {
        super(info);
        // e.g. http://localhost:8000/api/v1/curricula/metadatasets
        Matcher matched = matches("(https?:\\/\\/.*\\/)api\\/v1\\/curricula\\/metadatasets.*");
        if(matched.matches()){
            url = info.getValue();
            logger.info("matched Curriculum at "+matched.group(1));
        }
    }

    public ValuespaceData getValuespace(String locale) throws Exception {
        List<MetadataKey> result = new ArrayList<>();
        JSONArray list = getApi();
        for (int i = 0; i < list.length(); i++) {
            JSONArray subList = list.getJSONArray(i);
            for (int j = 0; j < subList.length(); j++) {
                JSONObject entry = subList.getJSONObject(j);
                result.add(convertEntry(entry));
            }
        }
        return new ValuespaceData(null, result);
    }

    private MetadataKey convertEntry(JSONObject entry) throws JSONException {
        MetadataKey key = new MetadataKey();
        key.setCaption(entry.getString("title"));
        key.setKey(entry.getString("id"));
        key.setParent(entry.isNull("parent_id") ? null : entry.getString("parent_id"));
        return key;
    }

    private JSONArray getApi() throws IOException, JSONException {
        return new JSONArray(ReaderUtils.query(getApiUrl()));
    }
    private String getApiUrl() {
        return url;
    }

    @Override
    protected boolean supportsUrl() {
        return url != null;
    }
}
