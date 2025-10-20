package org.edu_sharing.repository.server.update;

import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface UpdaterService {
    Collection<UpdateInfo> getUpdateInfo();

    @RunAsSystem
    void runUpdate(@NotNull String updateId);

    @RunAsSystem
    void testUpdate(String updateId);

    void setApplicationContext(org.springframework.context.ApplicationContext applicationContext);
}
