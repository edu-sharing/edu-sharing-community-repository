import { Injectable } from '@angular/core';
import { WorkspaceManagementDialogsComponent } from './management-dialogs.component';

/**
 * Provides access to dialogs via a global component.
 *
 * @deprecated will be replaced by overlay-based dialogs available through `DialogsService`.
 */
@Injectable({
    providedIn: 'root',
})
export class ManagementDialogsService {
    private dialogsComponent: WorkspaceManagementDialogsComponent;

    constructor() {}

    getDialogsComponent(): WorkspaceManagementDialogsComponent {
        return this.dialogsComponent;
    }

    registerDialogsComponent(dialogsComponent: WorkspaceManagementDialogsComponent): void {
        this.dialogsComponent = dialogsComponent;
    }
}
