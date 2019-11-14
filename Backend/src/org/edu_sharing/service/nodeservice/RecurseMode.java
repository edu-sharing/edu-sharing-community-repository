package org.edu_sharing.service.nodeservice;

public enum RecurseMode {
    Folders, // Only recursing folders, so not childobjects, comments, ratings etc. are returned (default)
    All, // will recurse everything until no more children are available
}
