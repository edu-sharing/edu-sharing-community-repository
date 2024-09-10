package org.edu_sharing.repository.server.connector;

import org.apache.http.client.methods.RequestBuilder;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.junit.jupiter.api.Test;

class ConnectorServletTest {
    @Test
    void name() {
        RequestBuilder post = RequestBuilder.post("https://curriculum-dev.schulcampus-rlp.de/oauth/token");
        post.addParameter("grant_type", "client_credentials");
        post.addParameter("client_id", "4");
        post.addParameter("client_secret", "1w1dI8wi5FsQ2sAA71V5ug4GBfHpfTFhYuN1Caia");
        String result = new HttpQueryTool().query(post);
    }
}