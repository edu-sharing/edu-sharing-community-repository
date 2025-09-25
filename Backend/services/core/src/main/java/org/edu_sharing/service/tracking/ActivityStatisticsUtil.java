package org.edu_sharing.service.tracking;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.tracking.statistics.ActivityStatisticsConfig;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatisticsUtil {

    private static final String SESSION_AUTHORITY_MEDIACENTERS = "SESSION_AUTHORITY_MEDIACENTERS";
    private static final String SESSION_AUTHORITY_ORGANIZATIONS = "SESSION_AUTHORITY_ORGANIZATIONS";

    private final ActivityStatisticsConfig config;


    public String getTrackedUsername(String username) {
        if (username == null) {
            username = AuthenticationUtil.getFullyAuthenticatedUser();
        }

        UserTrackingMode mode = config.getUserMode() == null ? UserTrackingMode.none : config.getUserMode();
        if (mode.equals(UserTrackingMode.obfuscate)) {
            return DigestUtils.sha1Hex(username);
        } else if (mode.equals(UserTrackingMode.full)) {
            return username;
        } else if (mode.equals(UserTrackingMode.session)) {
            HttpSession session = getSession();
            if (session != null) {
                return DigestUtils.sha1Hex(session.getId() + username);
            }
        }

        // we need any kind of stable id for tracking, so we'll generate a random, hopefully unique UUID
        return UUID.randomUUID().toString();
    }

    private HttpSession getSession() {
        if (Context.getCurrentInstance() != null && Context.getCurrentInstance().getRequest() != null) {
            return Context.getCurrentInstance().getRequest().getSession(false);
        }
        return null;
    }

    public String[] getAuthorityOrganizations() {
        try {
            if (ContextManagementFilter.accessTool.get() == null || ContextManagementFilter.accessTool.get().getUserId() == null) {
                // use the fully authenticated user since the current runAs user might be system
                HttpSession session = getSession();
                String[] result;
                if (session != null) {
                    result = (String[]) session.getAttribute(SESSION_AUTHORITY_ORGANIZATIONS);
                    if (result != null) {
                        return result;
                    }
                }
                result = AuthenticationUtil.runAs(
                        () -> SearchServiceFactory.getInstance().getLocalService().getAllOrganizations(true).getData().stream().map(EduGroup::getGroupname).toArray(String[]::new),
                        AuthenticationUtil.getFullyAuthenticatedUser()
                );
                if (session != null) {
                    session.setAttribute(SESSION_AUTHORITY_ORGANIZATIONS, result);
                }
                return result;
            } else {
                return AuthenticationUtil.runAs(
                        () -> SearchServiceFactory.getInstance().getLocalService().getAllOrganizations(true).getData().stream().map(EduGroup::getGroupname).toArray(String[]::new),
                        ContextManagementFilter.accessTool.get().getUserId()
                );
            }
        } catch (Exception e) {
            log.info("Failed to track organizations of user", e);
            return null;
        }
    }

    public String[] getAuthorityMediacenters() {
        try {
            if (ContextManagementFilter.accessTool.get() == null || ContextManagementFilter.accessTool.get().getUserId() == null) {
                // use the fully authenticated user since the current runAs user might be system
                HttpSession session = getSession();
                String[] result;
                if (session != null) {
                    result = (String[]) session.getAttribute(SESSION_AUTHORITY_MEDIACENTERS);
                    if (result != null) {
                        return result;
                    }
                }
                result = AuthenticationUtil.runAs(
                        () -> SearchServiceFactory.getInstance().getLocalService().getAllMediacenters(true).toArray(String[]::new),
                        AuthenticationUtil.getFullyAuthenticatedUser()
                );
                if (session != null) {
                    session.setAttribute(SESSION_AUTHORITY_MEDIACENTERS, result);
                }
                return result;
            } else {
                return AuthenticationUtil.runAs(
                        () -> SearchServiceFactory.getInstance().getLocalService().getAllMediacenters(true).toArray(String[]::new),
                        ContextManagementFilter.accessTool.get().getUserId()
                );
            }
        } catch (Exception e) {
            log.info("Failed to track mediacenters of user", e);
            return null;
        }
    }
}
