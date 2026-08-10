package org.edu_sharing.service.connector;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import java.net.URLEncoder;
import java.util.Map;

/**
 * replaces the attributes/variables that are supported in the urls and bodies of a simple connector,
 * i.e. {{parameter}} of the given parameters as well as the global query variables (like ${user.<property>})
 */
@Slf4j
public class SimpleConnectorAttributes {

    public static final String ATTRIBUTE_NODE_ID = "nodeId";
    public static final String ATTRIBUTE_ORIGINAL_NODE_ID = "originalNodeId";

    public interface Formatter {
        String format(String[] value);
    }

    /**
     * replace attributes for usage inside an url, i.e. the values are url encoded
     */
    public static String replaceForUrl(Map<String, String[]> parameters, String strToReplace) {
        return replace(parameters, strToReplace, (data) -> URLEncoder.encode(StringUtils.join(data)));
    }

    public static String replace(Map<String, String[]> parameters, String strToReplace, Formatter format) {
        for (Map.Entry<String, String[]> parameter : parameters.entrySet()) {
            strToReplace = strToReplace.replace("{{" + parameter.getKey() + "}}", format.format(parameter.getValue()));
        }
        // allow global variables that are also allowed for queries
        strToReplace = MetadataSearchHelper.replaceCommonQueryVariables(strToReplace);
        // replace other, unknown attributes with empty value
        strToReplace = strToReplace.replaceAll("\\{\\{.*}}", "");
        return strToReplace;
    }

    /**
     * the id of the original node of a collection reference, i.e. the value of the
     * {{originalNodeId}} attribute. Falls back to the given node id if the node is no reference
     * (runs as system since the current user may not have permissions on the original)
     */
    public static String resolveReferenceOriginalNodeId(String nodeId) {
        try {
            return AuthenticationUtil.runAsSystem(() ->
                    NodeServiceFactory.getInstance().getLocalService().getReferenceOriginalNode(nodeId).getId());
        } catch (Throwable t) {
            log.warn("Could not resolve original node id for {}: {}", nodeId, t.getMessage(), t);
            return nodeId;
        }
    }
}
