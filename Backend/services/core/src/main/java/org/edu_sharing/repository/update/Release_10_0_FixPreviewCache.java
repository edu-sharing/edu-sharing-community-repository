package org.edu_sharing.repository.update;


import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;

@Slf4j
@UpdateService
public class Release_10_0_FixPreviewCache {

    public Release_10_0_FixPreviewCache(){
    }

    @UpdateRoutine(
            id = "Release_10_0_FixPreviewCache",
            description = "Clear all previews from the none-purged cache m_1200x1200",
            order = 0,
            auto = true)
    public void execute() {
        PreviewCache.purgeCacheFolder(0, 0, 1200, 1200);
    }
}
