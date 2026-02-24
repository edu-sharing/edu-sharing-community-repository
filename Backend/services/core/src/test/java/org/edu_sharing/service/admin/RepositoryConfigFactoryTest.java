package org.edu_sharing.service.admin;

import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RepositoryConfigFactoryTest {

    @Test
    void getSystemMessageTest() {
        long now = System.currentTimeMillis();
        RepositoryConfig config = new RepositoryConfig();
        AuthorityService authority = mock(AuthorityService.class);
        RepositoryConfig.RepositoryMessage msg = new RepositoryConfig.RepositoryMessage();
        config.setMessages(List.of(msg));

        try (MockedStatic<RepositoryConfigFactory> svc = mockStatic(RepositoryConfigFactory.class, CALLS_REAL_METHODS);
             MockedStatic<AuthorityServiceFactory> auth = mockStatic(AuthorityServiceFactory.class)) {
            svc.when(RepositoryConfigFactory::getConfig).thenReturn(config);
            auth.when(AuthorityServiceFactory::getLocalService).thenReturn(authority);

            when(authority.isGuest()).thenReturn(false);
            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.user);
            assertEquals(msg, RepositoryConfigFactory.getSystemMessage());
            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.guest);
            assertNull(RepositoryConfigFactory.getSystemMessage());


            when(authority.isGuest()).thenReturn(true);
            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.guest);
            assertEquals(msg, RepositoryConfigFactory.getSystemMessage());
            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.user);
            assertNull(RepositoryConfigFactory.getSystemMessage());

            msg.setUserMode(RepositoryConfig.RepositoryMessage.UserMode.all);
            msg.setFrom(now - 10000);
            assertEquals(msg, RepositoryConfigFactory.getSystemMessage());
            msg.setFrom(now + 10000);
            assertNull(RepositoryConfigFactory.getSystemMessage());

            msg.setFrom(null);
            msg.setTo(now + 10000);
            assertEquals(msg, RepositoryConfigFactory.getSystemMessage());
            msg.setTo(now - 10000);
            assertNull(RepositoryConfigFactory.getSystemMessage());
        }
    }
}