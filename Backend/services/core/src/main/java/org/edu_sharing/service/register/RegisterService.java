package org.edu_sharing.service.register;

import org.edu_sharing.restservices.register.v1.model.RegisterInformation;

import java.security.InvalidKeyException;

public interface RegisterService {
    void resetPassword(String key, String newPassword) throws Exception;

    boolean recoverPassword(String id) throws Exception;

    boolean userExists(String mail) throws Exception;

    String activate(String key) throws InvalidKeyException,Throwable;

    void register(RegisterInformation info) throws DuplicateAuthorityException, Throwable;

    boolean resendRegisterMail(String mail) throws Exception;

    class DuplicateAuthorityException extends Exception{

    }
}
