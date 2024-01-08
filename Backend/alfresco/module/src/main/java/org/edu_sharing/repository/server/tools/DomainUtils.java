package org.edu_sharing.repository.server.tools;

import com.google.common.net.InternetDomainName;
import org.apache.commons.lang3.StringUtils;

public final class DomainUtils {

    public static String getRootDomain(String urlString) {
        if (StringUtils.isBlank(urlString)) {
            return null;
        }
        try {
            return InternetDomainName
                    .from(urlString)
                    .topPrivateDomain()
                    .toString();

        }catch (Exception ignore){
            return urlString;
        }
    }
}
