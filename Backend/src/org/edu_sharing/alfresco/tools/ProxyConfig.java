package org.edu_sharing.alfresco.tools;

import java.io.Serializable;

public class ProxyConfig implements Serializable {
    String host = null;
    String proxyhost = null;

    String proxyUsername = null;
    String proxyPass = null;

    Integer proxyport = null;

    String nonProxyHosts = null;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getProxyhost() {
        return proxyhost;
    }

    public void setProxyhost(String proxyhost) {
        this.proxyhost = proxyhost;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPass() {
        return proxyPass;
    }

    public void setProxyPass(String proxyPass) {
        this.proxyPass = proxyPass;
    }

    public Integer getProxyport() {
        return proxyport;
    }

    public void setProxyport(Integer proxyport) {
        this.proxyport = proxyport;
    }

    public String getNonProxyHosts() {
        return nonProxyHosts;
    }

    public void setNonProxyHosts(String nonProxyHosts) {
        this.nonProxyHosts = nonProxyHosts;
    }
}
