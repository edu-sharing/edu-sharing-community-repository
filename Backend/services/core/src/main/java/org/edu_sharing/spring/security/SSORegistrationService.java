package org.edu_sharing.spring.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class SSORegistrationService {

    private final Optional<InMemoryClientRegistrationRepository> clientRegistrationRepository;

    public SSORegistrationService(Optional<InMemoryClientRegistrationRepository> clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public List<ClientRegistration> getClientRegistrations(){
        return clientRegistrationRepository
                .map(clientRegistrations -> StreamSupport.stream(clientRegistrations.spliterator(), false).toList())
                .orElseGet(ArrayList::new);
    }

}
