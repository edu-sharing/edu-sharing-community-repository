package org.edu_sharing.service.tracking.user_tracking;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Fallback implementation, registered as a bean via {@link UserTrackingConfig} when no other
 * {@link UserNodeActivityDataService} (e.g. from plugin-mongo) is available. Must not be a
 * scanned {@code @Component}/{@code @Service} itself, since {@code @ConditionalOnMissingBean}
 * only works reliably on {@code @Bean} methods, not on scanned classes (see
 * {@link UserTrackingConfig}).
 */
@Slf4j
public class DefaultUserNodeActivityDataService implements UserNodeActivityDataService {

    @PostConstruct
    void init(){
        log.info("DefaultUserNodeActivityDataService is active");
    }

    @NotNull
    @Override
    public List<UserNodeActivity> getDataForAllUsers(@NotNull Date after, Date until, int limit) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<UserNodeActivity> getDataForUser(@NotNull String username, @NotNull Date after) {
        return Collections.emptyList();
    }
}
