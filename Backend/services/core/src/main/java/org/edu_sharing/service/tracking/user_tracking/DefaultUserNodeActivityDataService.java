package org.edu_sharing.service.tracking.user_tracking;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.spring.conditions.ConditionalOnMissingBean;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@ConditionalOnMissingBean(UserNodeActivityDataService.class)
public class DefaultUserNodeActivityDataService implements UserNodeActivityDataService {

    @PostConstruct
    void init(){
        log.info("DefaultUserNodeActivityDataService is active");
    }

    @NotNull
    @Override
    public Page<UserNodeActivity> getDataForAllUsers(@NotNull Date after, Pageable pageable) {
        return Page.empty();
    }

    @NotNull
    @Override
    public List<UserNodeActivity> getDataForUser(@NotNull String username, @NotNull Date after) {
        return Collections.emptyList();
    }
}
