package org.edu_sharing.restservices.admin.v1;

import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.repository.server.tools.ApplicationInfo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ApplicationSimple {
    private String id;
    private String title;
    private String type;
    private String subtype;
    private String host;
    private String domain;
    private List<String> allowedOrigins;


    public void fill(ApplicationInfo appInfo) {
        setId(appInfo.getAppId());
        setTitle(appInfo.getAppCaption());
        setType(appInfo.getType());
        setSubtype(appInfo.getSubtype());
        setHost(appInfo.getHost());
        setDomain(appInfo.getDomain());
        setAllowedOrigins(Arrays.stream(appInfo.getString(ApplicationInfo.KEY_ALLOW_ORIGIN, "").split(","))
                .filter(StringUtils::isNotBlank) // Remove nulls and blanks
                .map(String::trim)                             // Trim remaining strings
                .collect(Collectors.toList())
        );

    }
}