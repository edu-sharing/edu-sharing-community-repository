package org.edu_sharing.restservices;

import org.edu_sharing.restservices.register.v1.model.RegisterInformation;
import org.edu_sharing.service.register.RegisterServiceFactory;

public class RegisterDao {
    public static void register(RegisterInformation info) throws DAOException {
        try {
            RegisterServiceFactory.getLocalService().register(info);
        }catch(Throwable t){
            throw DAOException.mapping(t);
        }
    }

    public static void activate(String key) throws DAOException {
        try {
            RegisterServiceFactory.getLocalService().activate(key);
        }catch(Throwable t){
            throw DAOException.mapping(t);
        }
    }
}
