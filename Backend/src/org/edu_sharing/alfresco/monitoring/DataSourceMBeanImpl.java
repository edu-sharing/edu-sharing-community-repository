package org.edu_sharing.alfresco.monitoring;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(objectName = "Alfresco:type=DataSource")
public class DataSourceMBeanImpl extends MBeanSupport implements DataSourceMBean {

    private BasicDataSource dataSource;

    public void setDataSource(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    @ManagedAttribute
    public int getActiveConnections() {
        return doWork(() -> dataSource.getNumActive());
    }

    @Override
    @ManagedAttribute
    public int getIdleConnections() {
        return doWork(() -> dataSource.getNumIdle());
    }

}
