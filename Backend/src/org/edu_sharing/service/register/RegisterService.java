package org.edu_sharing.service.register;

import org.edu_sharing.restservices.register.v1.model.RegisterInformation;

import java.security.InvalidKeyException;

public interface RegisterService {
    String activate(String key) throws InvalidKeyException,Throwable;

    void register(RegisterInformation info) throws DuplicateAuthorityException, Throwable;

    boolean resendRegisterMail(String mail) throws Exception;

    class DuplicateAuthorityException extends Exception{

    }
}
