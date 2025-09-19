package org.edu_sharing.repository.server.appcontext;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.config.ConfigServiceFactory;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApplicationInfoContextFilter implements ContainerRequestFilter, CompletionCallback {

    private static final String HOME = "-home-";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();
        Map<String, String> pathParams = uriInfo.getPathParameters()
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(0)
                ));

        String repositoryId = pathParams.get("repository");
        if (repositoryId == null || repositoryId.isBlank()) {
            ApplicationInfoContextHolder.clear();
            return;
        }

        try {

            ApplicationInfo appInfo = HOME.equals(repositoryId)
                    ? ApplicationInfoList.getHomeRepositoryObeyConfig(ConfigServiceFactory.getCurrentConfig().getValue("availableRepositories", null))
                    : ApplicationInfoList.getRepositoryInfoById(repositoryId);


            if (appInfo == null) {
                log.debug("");
                log.debug("Unbekanntes repository '{}', verwende Fallback (Home).", repositoryId);
                appInfo = ApplicationInfoList.getHomeRepository();
            }
            ApplicationInfoContextHolder.setCurrentApplicationInfo(appInfo);
        } catch (Throwable t) {
            log.warn("Konnte ApplicationInfo für repository '{}' nicht setzen: {}", repositoryId, t.getMessage());
        }
    }

    @Override
    public void onComplete(Throwable throwable) {
        ApplicationInfoContextHolder.clear();
    }
}
