package org.edu_sharing.restservices;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

/**
 * Logs the 400 Bad Request errors Jersey raises itself, e.g. for a broken multipart body before the
 * resource method is reached. Jersey answers these without any log entry.
 * Only observes; the response is not changed.
 */
@Slf4j
@Provider
public class RequestExceptionLoggingListener implements ApplicationEventListener {

    private static final RequestEventListener REQUEST_LISTENER = event -> {
        if (event.getType() != RequestEvent.Type.ON_EXCEPTION
                || !(event.getException() instanceof BadRequestException)) {
            return;
        }
        ContainerRequest request = event.getContainerRequest();
        log.warn("Bad request: {} {} (contentType={}, contentLength={}, userAgent={})",
                request == null ? null : request.getMethod(),
                request == null ? null : request.getRequestUri(),
                request == null ? null : request.getHeaderString("Content-Type"),
                request == null ? null : request.getHeaderString("Content-Length"),
                request == null ? null : request.getHeaderString("User-Agent"),
                event.getException());
    };

    @Override
    public void onEvent(ApplicationEvent event) {
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return REQUEST_LISTENER;
    }
}
