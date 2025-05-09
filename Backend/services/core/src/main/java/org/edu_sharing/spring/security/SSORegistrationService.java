package org.edu_sharing.spring.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class SSORegistrationService {

    @Autowired(required = false)
    @Lazy
    ClientRegistrationRepository clientRegistrationRepository;

    public List<ClientRegistration> getClientRegistrations(){
        if(clientRegistrationRepository == null){
            return new ArrayList<>();
        }
        if(!(clientRegistrationRepository instanceof InMemoryClientRegistrationRepository)){
            log.warn("clientRegistrationRepository is not an instance of InMemoryClientRegistrationRepository");
            return new ArrayList<>();
        }
        return StreamSupport.stream( ((InMemoryClientRegistrationRepository)clientRegistrationRepository).spliterator(), false)
                .collect(Collectors.toList());
    }

}
