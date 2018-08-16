package org.edu_sharing.restservices;

import org.edu_sharing.service.network.NetworkServiceFactory;
import org.edu_sharing.service.network.model.Service;
import org.edu_sharing.service.network.model.StoredService;

import java.util.Collection;

public class NetworkDao {
    public static Collection<StoredService> getServices() throws DAOException {
        try {
            return NetworkServiceFactory.getNetworkService().getServices();
        }catch(Throwable t){
            throw DAOException.mapping(t);
        }
    }

    public static StoredService addService(Service service) throws DAOException {
        try {
            return NetworkServiceFactory.getNetworkService().addService(service);
        }catch(Throwable t){
            throw DAOException.mapping(t);
        }
    }

    public static StoredService updateService(String id, Service service) throws DAOException {
        try {
            return NetworkServiceFactory.getNetworkService().updateService(id,service);
        }catch(Throwable t){
            throw DAOException.mapping(t);
        }
    }

    public static StoredService getOwnService() throws DAOException {
        try {
            return NetworkServiceFactory.getNetworkService().getOwnService();
        }catch(Throwable t){
            throw DAOException.mapping(t);
        }
    }
}
