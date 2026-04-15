package org.edu_sharing.alfresco.service.connector.bodyhandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.edu_sharing.alfresco.service.connector.SimpleConnector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CurriculumBodyHandler implements SimpleConnector.BodyHandler {

    @Override
    public List<BasicNameValuePair> handle(List<BasicNameValuePair> pairs, Map<String, String[]> requestParameters, SimpleConnector simpleConnector) {
        pairs.add(new BasicNameValuePair("title", getField(requestParameters, "curriculum_title")));
        pairs.add(new BasicNameValuePair("description", getField(requestParameters, "curriculum_description")));
        pairs.add(new BasicNameValuePair("color", getField(requestParameters, "curriculum_color")));
        pairs.add(new BasicNameValuePair("editable", getField(requestParameters, "curriculum_editable")));
        // boolean must be int
        pairs.add(new BasicNameValuePair("commentable", "true".equalsIgnoreCase(getField(requestParameters, "curriculum_commentable")) ? "true" : "false"));
        pairs.add(new BasicNameValuePair("auto_refresh", "true".equalsIgnoreCase(getField(requestParameters, "curriculum_auto_refresh")) ? "true" : "false"));
        pairs.add(new BasicNameValuePair("only_edit_owned_items", "true".equalsIgnoreCase(getField(requestParameters, "curriculum_only_edit_owned_items")) ? "true" : "false"));
        pairs.add(new BasicNameValuePair("collapse_items", "true".equalsIgnoreCase(getField(requestParameters, "curriculum_collapse_items")) ? "true" : "false"));
        pairs.add(new BasicNameValuePair("only_allow_copy", "true".equalsIgnoreCase(getField(requestParameters, "curriculum_only_allow_copy")) ? "true" : "false"));
        return pairs;
    }

    @Nullable
    private static String getField(Map<String, String[]> requestParameters, String field) {
        return StringUtils.join(requestParameters.get(field));
    }
}