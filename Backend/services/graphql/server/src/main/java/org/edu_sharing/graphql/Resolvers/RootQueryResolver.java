package org.edu_sharing.graphql.Resolvers;

import graphql.kickstart.tools.GraphQLQueryResolver;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.edu_sharing.graphql.domain.version.EduSharingInfo;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class RootQueryResolver implements GraphQLQueryResolver {
    protected ObjectMapper objectMapper = new ObjectMapper();
    public EduSharingInfo eduSharingInfo() throws IOException {
        // TODO
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet("http://localhost:8080/edu-sharing/version.json");

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }

            String jsonResponse = EntityUtils.toString(entity, "UTF-8");

            objectMapper.readTree(jsonResponse);
            return objectMapper.readValue(jsonResponse, EduSharingInfo.class);
        }
    }
}
