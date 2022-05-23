package org.edu_sharing.service.network;

import org.edu_sharing.service.network.model.Service;
import org.edu_sharing.service.network.model.StoredService;

import java.util.Collection;

public interface NetworkService {

	Collection<StoredService> getServices() throws Throwable;

    StoredService getOwnService();

    StoredService addService(Service service) throws Throwable;

    StoredService updateService(String id, Service service) throws Throwable;
}
