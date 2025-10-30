import { EventEmitter, Injectable, signal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { EditorialSidebarComponent, OptionState } from './editorial-sidebar.component';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class EditorialSidebarService {
    /**
     * triggered when in the sidebar a copy / apply event was performed
     */
    applyNodeEmitted = new EventEmitter<{ nodes: Node[]; parent?: Node }>();
    private _editorialSidebar: EditorialSidebarComponent;
    readonly sidebarOpened = signal(false);
    registerSidebar(editorialSidebar: EditorialSidebarComponent) {
        if (this._editorialSidebar && this._editorialSidebar !== editorialSidebar) {
            console.error(
                'Duplicate registration of editorial sidebar',
                this._editorialSidebar,
                editorialSidebar,
            );
            throw new Error('Duplicate registration of editorial sidebar');
        }
        this._editorialSidebar = editorialSidebar;
    }

    unregisterSidebar(editorialSidebar: EditorialSidebarComponent) {
        if (this._editorialSidebar !== editorialSidebar) {
            throw new Error('This sidebar is not registered');
        }
        this._editorialSidebar = null;
    }
    get editorialSidebar(): EditorialSidebarComponent {
        return this._editorialSidebar;
    }

    showOption(state: OptionState) {
        this._editorialSidebar.enabledOption.set(state);
        this.sidebarOpened.set(true);
    }
}
