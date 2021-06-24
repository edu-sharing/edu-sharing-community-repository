package org.edu_sharing.catalina.startup;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.startup.HostConfig;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class OrderedHostConfig extends HostConfig {

    private static final Comparator<? super String> COMPARE = String::compareTo;

    @Override
    protected void deployApps() {
        // Tomcat 7
        // File appBase = appBase();
        // Tomcat 8
        // File appBase = host.getAppBaseFile();
        // Tomcat 7+8
        File appBase = returnCanonicalPath(host.getAppBase());
        String[] sortedApps = appBase.list();
        Arrays.sort(sortedApps, COMPARE);

        // Tomcat 7
        // File configBase = configBase();
        // Tomcat 8
        // File configBase = host.getConfigBaseFile();
        // Tomcat 7+8
        File configBase = null;
        if (host.getXmlBase()!=null) {
            configBase = returnCanonicalPath(host.getXmlBase());
        } else {
            StringBuilder xmlDir = new StringBuilder("conf");
            Container parent = host.getParent();
            if (parent instanceof Engine) {
                xmlDir.append('/');
                xmlDir.append(parent.getName());
            }
            xmlDir.append('/');
            xmlDir.append(host.getName());
            configBase = returnCanonicalPath(xmlDir.toString());
        }
        String[] sortedContext = configBase.list();
        Arrays.sort(sortedContext, COMPARE);

        String[] filteredAppPaths = filterAppPaths(sortedApps);
        // Deploy XML descriptors from configBase
        deployDescriptors(configBase, sortedContext);
        // Deploy WARs
        deployWARs(appBase, filteredAppPaths);
        // Deploy expanded folders
        deployDirectories(appBase, filteredAppPaths);
    }
}