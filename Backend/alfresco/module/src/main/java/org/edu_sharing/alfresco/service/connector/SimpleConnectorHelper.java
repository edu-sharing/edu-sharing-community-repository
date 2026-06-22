package org.edu_sharing.alfresco.service.connector;

import com.typesafe.config.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.edu_sharing.repository.server.tools.http.HttpQueryTool;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleConnectorHelper {
    public static void addAuthentication(SimpleConnector simpleConnector, RequestBuilder builder) throws UnsupportedEncodingException {
        if(simpleConnector.getApi().getAuthentication() != null && simpleConnector.getApi().getAuthentication().getType() != null) {
            RequestBuilder builderAuth = null;
            SimpleConnector.SimpleConnectorAuthentication authentication = simpleConnector.getApi().getAuthentication();
            if (SimpleConnector.SimpleConnectorApi.Method.Post.equals(authentication.getMethod())) {
                builderAuth = RequestBuilder.post(authentication.getUrl());
                if (SimpleConnector.SimpleConnectorApi.BodyType.Form.equals(authentication.getBodyType())) {
                    List<? extends NameValuePair> data = authentication.getBody().entrySet().stream().map((e) -> new BasicNameValuePair(e.getKey(), e.getValue().toString())).collect(Collectors.toList());
                    builderAuth.setEntity(new UrlEncodedFormEntity(data));
                    builderAuth.setHeader("Content-Type", "application/x-www-form-urlencoded");
                }
                // builder.setHeader()
            }
            String auth = "";
            try {
                auth = new HttpQueryTool().query(builderAuth);
                JSONObject authJson = new JSONObject(auth);
                builder.setHeader("Authorization", "Bearer " + authJson.get("access_token"));
            }catch(JSONException e) {
                throw new IllegalArgumentException("Wrong json data received: " + auth, e);
            }
        }
    }

}
