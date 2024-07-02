package org.edu_sharing.service.guest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.lightbend.ConfigurationPropertyClassFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestService {
    private final ConfigurationPropertyClassFactory configurationPropertyClassFactory;

    private final PersonService personService;
    private final MutableAuthenticationService authenticationService;
    private final AuthorityService authorityService;

    private GuestConfig getDefaultConfig(){
        return getConfig(null);
    }

    private GuestConfig getConfig(String context) {
        return configurationPropertyClassFactory.getConfiguration(GuestConfig.class, new GuestConfigOption(context));
    }

    private NodeRef createOrUpdateGuest(GuestConfig guestConfig) {
        NodeRef guestRef = personService.getPersonOrNull(guestConfig.username);
        if(guestRef == null) {
            Map<QName, Serializable> properties = Map.of(
                    ContentModel.PROP_USERNAME, guestConfig.username,
                    ContentModel.PROP_FIRSTNAME, guestConfig.username,
                    QName.createQName(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION), CCConstants.CM_VALUE_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION_GUEST);

            authenticationService.createAuthentication(guestConfig.username, guestConfig.password.toCharArray());
            guestRef = personService.createPerson(properties);
        }

        Set<String> currentMemberships = authorityService.getMemberships(guestConfig.username);

        Set<String> toRemove = new HashSet<>(currentMemberships);
        guestConfig.getGroups().forEach(toRemove::remove);

        Set<String> toCreate = new HashSet<>(guestConfig.getGroups());
        toCreate.removeAll(currentMemberships);

        final String[] members = new String[]{guestConfig.username};
        toRemove.forEach(x->authorityService.removeMemberships(x, members));
        toCreate.forEach(x->authorityService.addMemberships(x, members));
        return guestRef;
    }


}
