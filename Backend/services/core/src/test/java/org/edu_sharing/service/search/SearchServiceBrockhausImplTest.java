package org.edu_sharing.service.search;

import org.edu_sharing.repository.server.appcontext.ApplicationInfoContextHolder;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceBrockhausImplTest {
    @Mock
    private ApplicationInfo appInfo;
    private MockedStatic<ApplicationInfoContextHolder> applicationInfoContextHolderMockedStatic;

    @BeforeEach
    void setup() {
        applicationInfoContextHolderMockedStatic = Mockito.mockStatic(ApplicationInfoContextHolder.class);
        applicationInfoContextHolderMockedStatic.when(ApplicationInfoContextHolder::getCurrentApplicationInfo).thenReturn(appInfo);

    }

    @AfterEach
    void teardown() {
        applicationInfoContextHolderMockedStatic.close();
    }

    @Test
    void buildUrl() {
        when(appInfo.getApiKey()).thenReturn("key");

        when(appInfo.getString("vidisIdp","")).thenReturn("");
        assertEquals("https://www.brockhaus.de/portal/user/key?url=%2fecs%2f123",SearchServiceBrockhausImpl.buildUrl("123"));
        assertEquals("https://www.brockhaus.de/portal/user/key?url=%2fjunior%2fecs%2f%2fkilex%2f123",SearchServiceBrockhausImpl.buildUrl("%2fkilex/123"));

        when(appInfo.getString("vidisIdp","")).thenReturn("idp");
        assertEquals("https://brockhaus.de/login?vidis_idp_hint=idp&url=https%3a%2f%2fbrockhaus.de%2fecs%2f123",SearchServiceBrockhausImpl.buildUrl("123"));
        assertEquals("https://brockhaus.de/login?vidis_idp_hint=idp&url=https%3a%2f%2fbrockhaus.de%2fjunior%2fecs%2f%2fkilex%2f123",SearchServiceBrockhausImpl.buildUrl("%2fkilex/123"));
    }
}
