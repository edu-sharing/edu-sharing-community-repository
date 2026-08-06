package org.edu_sharing.service.connector;

import org.apache.commons.lang.StringUtils;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;

import java.net.URLEncoder;
import java.util.Map;

/**
 * replaces the attributes/variables that are supported in the urls and bodies of a simple connector,
 * i.e. {{parameter}} of the given parameters as well as the global query variables (like ${user.<property>})
 */
public class SimpleConnectorAttributes {

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
}
