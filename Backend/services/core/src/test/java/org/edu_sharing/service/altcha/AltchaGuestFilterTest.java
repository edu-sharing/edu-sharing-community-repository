package org.edu_sharing.service.altcha;

import com.typesafe.config.ConfigValueFactory;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.alfresco.repo.cache.DefaultSimpleCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AltchaGuestFilterTest {

    private AltchaService altchaService;
    private ContainerRequestContext request;

    @BeforeEach
    void setUp() {
        altchaService = new AltchaService(() -> AltchaServiceTest.config(AltchaServiceTest.SECRET), new DefaultSimpleCache<>());
        request = mock(ContainerRequestContext.class);
    }

    private AltchaGuestFilter filter(boolean guest) {
        return new AltchaGuestFilter(() -> altchaService, () -> guest);
    }

    private int abortedStatus() {
        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(request).abortWith(response.capture());
        return response.getValue().getStatus();
    }

    @Test
    void authenticatedRequestWithoutAltchaPasses() {
        filter(false).filter(request);
        verify(request, never()).abortWith(any());
    }

    @Test
    void guestRequestWithoutAltchaIsForbidden() {
        filter(true).filter(request);
        assertEquals(403, abortedStatus());
    }

    @Test
    void guestRequestWithValidAltchaPassesOnce() throws Exception {
        when(request.getHeaderString(AltchaService.HEADER)).thenReturn(AltchaServiceTest.validPayload(altchaService));
        AltchaGuestFilter filter = filter(true);

        filter.filter(request);
        verify(request, never()).abortWith(any());

        filter.filter(request);
        assertEquals(409, abortedStatus());
    }

    @Test
    void disabledSkipsCheck() {
        altchaService = new AltchaService(() -> AltchaServiceTest.config(AltchaServiceTest.SECRET)
                .withValue("enabled", ConfigValueFactory.fromAnyRef(false)), new DefaultSimpleCache<>());
        filter(true).filter(request);
        verify(request, never()).abortWith(any());
    }
}
