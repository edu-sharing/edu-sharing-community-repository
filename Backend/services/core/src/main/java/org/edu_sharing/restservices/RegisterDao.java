package org.edu_sharing.restservices;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.restservices.register.v1.model.RegisterExists;
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
    public static void recoverPassword(String mail) throws DAOException {
        try {
            if(!RegisterServiceFactory.getLocalService().recoverPassword(mail))
                throw new SecurityException("Invalid mail address");
        }catch(Throwable t){
            throw DAOException.mapping(t);
        }
    }
    public static void resetPassword(String key,String newPassword) throws DAOException {
        try {
            RegisterServiceFactory.getLocalService().resetPassword(key,newPassword);
        }catch(Throwable t){
            throw DAOException.mapping(t);
        }
    }
    public static void resendMail(String mail) throws DAOException {
        try {
            if(!RegisterServiceFactory.getLocalService().resendRegisterMail(mail)){
                throw new SecurityException("Invalid mail address");
            }
        }catch(Throwable t){
            throw DAOException.mapping(t);
        }
    }

    public static RegisterExists mailExists(String mail) throws DAOException {
        try {
            RegisterExists exists = new RegisterExists();
            AuthenticationUtil.runAsSystem(() -> {
                exists.setExists(RegisterServiceFactory.getLocalService().userExists(mail));
                return null;
            });
            return exists;
        }catch(Exception e){
            throw DAOException.mapping(e);
        }
    }
}
