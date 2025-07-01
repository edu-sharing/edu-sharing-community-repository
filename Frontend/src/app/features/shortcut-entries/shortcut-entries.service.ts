import { Injectable } from '@angular/core';
import { HOME_REPOSITORY, IamV1Service, ME } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { DefaultOrRefShortcut, ExtendedShortcutEntry } from './shortcut-entries-types';

@Injectable({
    providedIn: 'root',
})
export class ShortcutEntriesService {
    constructor(private iamApi: IamV1Service) {}

    /**
     * Retrieves the dashboard shortcut entries of the current user.
     */
    async retrieveEntries() {
        return firstValueFrom(
            this.iamApi.getDashboardShortcuts({
                repository: HOME_REPOSITORY,
                person: ME,
            }),
        );
    }

    /**
     * Saves the specified entries for the current user.
     *
     * @param entries
     */
    async saveEntries(entries: ExtendedShortcutEntry[]) {
        const entriesToSave: DefaultOrRefShortcut[] = [];
        entries.forEach((entry) => {
            const entryToAdd: DefaultOrRefShortcut = {
                type: !!entry.node ? 'ref' : 'default',
            };
            if (!!entry.title) {
                entryToAdd.title = entry.title;
            }
            if (entryToAdd.type === 'ref') {
                entryToAdd.ref = 'workspace://SpacesStore/' + entry.node.ref.id;
            } else {
                entryToAdd.id = entry.id;
            }
            entriesToSave.push(entryToAdd);
        });
        await firstValueFrom(
            this.iamApi.setDashboardShortcuts({
                repository: HOME_REPOSITORY,
                person: ME,
                body: entriesToSave,
            }),
        );
    }
}
