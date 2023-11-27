package org.edu_sharing.repository.server.tools;

import com.google.common.net.InternetDomainName;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

public final class DomainUtils {

    public static String getRootDomain(String urlString){
        if(StringUtils.isBlank(urlString)){
           return null;
        }

        return  InternetDomainName
                .from(urlString)
                .topPrivateDomain()
                .name();

    }
}
