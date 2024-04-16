package org.edu_sharing.service.bulk;

public enum BulkUpdateBehaviour {
    /**
     * do nothing. New elements will simply be put in the same structur as exisiting ones
     */
    None,
    /**
     * new elements will be moved to a new folder representing the date of the import
     * This folder will inherit all permissions except GROUP_EVERYONE (if applicable)
     */
    SeparateViaFolder
}
