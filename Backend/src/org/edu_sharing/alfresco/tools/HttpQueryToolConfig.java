package org.edu_sharing.alfresco.tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class HttpQueryToolConfig implements Serializable {

    ProxyConfig proxyConfig = new ProxyConfig();
    List<String> disableSNI4Hosts = new ArrayList<>();

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public List<String> getDisableSNI4Hosts() {return disableSNI4Hosts;}

    public void setDisableSNI4Hosts(List<String> disableSNI4Hosts) {this.disableSNI4Hosts = disableSNI4Hosts;}


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
}
