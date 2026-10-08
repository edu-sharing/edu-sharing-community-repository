package org.edu_sharing.service.altcha;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Enforces {@link RequireAltchaForGuests}: guest (or unauthenticated) requests must provide a valid ALTCHA payload,
 * authenticated users skip the check.
 */
@Provider
@RequireAltchaForGuests
@Priority(Priorities.AUTHORIZATION)
public class AltchaGuestFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    private final Supplier<AltchaService> altchaService;
    private final BooleanSupplier isGuestRequest;

    public AltchaGuestFilter() {
        this(
                () -> ApplicationContextFactory.getApplicationContext().getBean(AltchaService.class),
                AltchaGuestFilter::isGuestRequest
        );
    }

    AltchaGuestFilter(Supplier<AltchaService> altchaService, BooleanSupplier isGuestRequest) {
        this.altchaService = altchaService;
        this.isGuestRequest = isGuestRequest;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        AltchaService service = altchaService.get();
        if (!service.isEnabled() || !isGuestRequest.getAsBoolean()) {
            return;
        }
        try {
            service.verify(requestContext.getHeaderString(AltchaService.HEADER), getAction());
        } catch (AltchaVerificationException e) {
            requestContext.abortWith(Response.status(e.getReason().getStatus()).build());
        }
    }

    private String getAction() {
        Method method = resourceInfo == null ? null : resourceInfo.getResourceMethod();
        RequireAltchaForGuests annotation = method == null ? null : method.getAnnotation(RequireAltchaForGuests.class);
        if (annotation == null && resourceInfo != null && resourceInfo.getResourceClass() != null) {
            annotation = resourceInfo.getResourceClass().getAnnotation(RequireAltchaForGuests.class);
        }
        return annotation == null ? AltchaService.ACTION_REPORT : annotation.value();
    }

    private static boolean isGuestRequest() {
        return AuthenticationUtil.getFullyAuthenticatedUser() == null
                || AuthorityServiceFactory.getInstance().getLocalService().isGuest();
    }
}
