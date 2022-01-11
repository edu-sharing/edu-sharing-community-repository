package org.edu_sharing.service.nodeservice;

import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;

public enum RecurseMode {
    @JobFieldDescription(description = "Only recursing folders, so no childobjects, comments, ratings etc. are returned (default)")
    Folders,
    @JobFieldDescription(description = "recurse everything until no more children are available")
    All,
}
