package org.edu_sharing.service.admin;

import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.*;

class RepositoryConfigFactoryTest {

    @Test
    void getSystemMessageTest() {
        long now = System.currentTimeMillis();
        RepositoryConfig config = new RepositoryConfig();
        AuthorityServiceFactory authorityServiceFactory = mock(AuthorityServiceFactory.class);
        AuthorityService authority = mock(AuthorityService.class);
        RepositoryConfig.RepositoryMessage msg = new RepositoryConfig.RepositoryMessage();
        config.setMessages(List.of(msg));

        try (MockedStatic<RepositoryConfigFactory> svc = mockStatic(RepositoryConfigFactory.class, CALLS_REAL_METHODS);
             MockedStatic<AuthorityServiceFactory> auth = mockStatic(AuthorityServiceFactory.class)) {
            svc.when(RepositoryConfigFactory::getConfig).thenReturn(config);
            auth.when(AuthorityServiceFactory::getInstance).thenReturn(authorityServiceFactory);
            when(authorityServiceFactory.getLocalService()).thenReturn(authority);

            when(authority.isGuest()).thenReturn(false);
            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.user);
            assertEquals(msg, RepositoryConfigFactory.getSystemMessages().get(0));
            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.guest);
            assertIterableEquals(Collections.emptyList(), RepositoryConfigFactory.getSystemMessages());


            when(authority.isGuest()).thenReturn(true);
            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.guest);
            assertEquals(msg, RepositoryConfigFactory.getSystemMessages().get(0));
            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.user);
            assertIterableEquals(Collections.emptyList(), RepositoryConfigFactory.getSystemMessages());

            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.all);
            msg.setFrom(now - 10000);
            assertEquals(msg, RepositoryConfigFactory.getSystemMessages().get(0));
            msg.setFrom(now + 10000);
            assertIterableEquals(Collections.emptyList(), RepositoryConfigFactory.getSystemMessages());

            msg.setFrom(null);
            msg.setTo(now + 10000);
            assertEquals(msg, RepositoryConfigFactory.getSystemMessages().get(0));
            msg.setTo(now - 10000);
            assertIterableEquals(Collections.emptyList(), RepositoryConfigFactory.getSystemMessages());
        }
    }
}