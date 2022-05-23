package org.edu_sharing.alfresco.monitoring;

public interface DataSourceMBean {

    int getActiveConnections();

    int getIdleConnections();

}
