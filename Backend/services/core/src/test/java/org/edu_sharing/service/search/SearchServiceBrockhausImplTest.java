package org.edu_sharing.service.search;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SearchServiceBrockhausImplTest {
    @Mock
    private ApplicationInfo appInfo;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this); // Initialize mocks and inject into userService
    }

    @Test
    void buildUrl() {
        when(appInfo.getAppId()).thenReturn("app_id");
        when(appInfo.getApiKey()).thenReturn("key");

        when(appInfo.getString("vidisIdp","")).thenReturn("");
        assertEquals("https://www.brockhaus.de/portal/user/key?url=%2fecs%2f123",SearchServiceBrockhausImpl.buildUrl(appInfo, "123"));
        assertEquals("https://www.brockhaus.de/portal/user/key?url=%2fjunior%2fecs%2f%2fkilex%2f123",SearchServiceBrockhausImpl.buildUrl(appInfo, "%2fkilex/123"));

        when(appInfo.getString("vidisIdp","")).thenReturn("idp");
        assertEquals("https://brockhaus.de/login?vidis_idp_hint=idp&url=https%3a%2f%2fbrockhaus.de%2fecs%2f123",SearchServiceBrockhausImpl.buildUrl(appInfo, "123"));
        assertEquals("https://brockhaus.de/login?vidis_idp_hint=idp&url=https%3a%2f%2fbrockhaus.de%2fjunior%2fecs%2f%2fkilex%2f123",SearchServiceBrockhausImpl.buildUrl(appInfo, "%2fkilex/123"));
    }
}