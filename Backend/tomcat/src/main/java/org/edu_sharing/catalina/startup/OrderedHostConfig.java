package org.edu_sharing.catalina.startup;

import org.apache.catalina.startup.HostConfig;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class OrderedHostConfig extends HostConfig {

    private static final Comparator<? super String> COMPARE = String::compareTo;

    @Override
    protected void deployApps() {
        File appBase = appBase();
        String[] sortedApps = appBase.list();
        Arrays.sort(sortedApps, COMPARE);

        File configBase = configBase();
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