import { ContributorData } from 'ngx-edu-sharing-api';

export interface ContributorRegistryEditDialogData {
    /** the contributor to edit; undefined => create a new one */
    contributor?: ContributorData;
}

export interface ContributorRegistryEditDialogResult {
    /** the edited field values */
    contributor: Partial<ContributorData>;
    /** whether the change should be propagated to all media (only relevant on edit) */
    applyToExisting: boolean;
}
