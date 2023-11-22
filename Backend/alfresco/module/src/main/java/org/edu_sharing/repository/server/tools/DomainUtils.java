package org.edu_sharing.repository.server.tools;

import com.google.common.net.InternetDomainName;

import java.net.MalformedURLException;
import java.net.URL;

public final class DomainUtils {

    public static String getRootDomain(String urlString){
        try {
            return  InternetDomainName
                    .from(new URL(urlString).getHost())
                    .topPrivateDomain()
                    .name();

        } catch (MalformedURLException ignored) {
            return null;
        }
    }
}
