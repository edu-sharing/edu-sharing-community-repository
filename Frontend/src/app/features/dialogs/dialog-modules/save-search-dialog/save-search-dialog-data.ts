import { SavedSearch } from 'ngx-edu-sharing-api';

export interface SaveSearchDialogData {
    name: string;
    searchString: string;
}

export type SaveSearchDialogResult = SavedSearch | null;
