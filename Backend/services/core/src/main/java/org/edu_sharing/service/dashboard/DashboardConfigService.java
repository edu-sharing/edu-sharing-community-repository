package org.edu_sharing.service.dashboard;

import org.edu_sharing.service.dashboard.models.DashboardShortcut;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface DashboardConfigService {

    @Cacheable(value = "dashboardShortcuts", key = "#username")
    List<DashboardShortcut> getDashboardShortcuts(String username);

    @CacheEvict(value = "dashboardShortcuts", key = "#username")
    void setDashboardShortcuts(String username, List<DashboardShortcut> shortcuts);

    List<DashboardShortcut> getDefaultDashBoardShortCuts();
}
