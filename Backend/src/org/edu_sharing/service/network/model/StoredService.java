package org.edu_sharing.service.network.model;

import org.apache.commons.beanutils.PropertyUtils;

public class StoredService extends Service {
    private String id;

    public StoredService(String id, Service service) {
        try {
            PropertyUtils.copyProperties(this,service);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.id=id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
