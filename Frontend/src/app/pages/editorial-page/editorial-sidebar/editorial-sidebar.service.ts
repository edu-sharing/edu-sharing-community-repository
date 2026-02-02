import { EventEmitter, Injectable, signal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import {
    EditorialSidebarComponent,
    OptionConfig,
    OptionState,
} from './editorial-sidebar.component';

@Injectable({
    providedIn: 'root',
})
export class EditorialSidebarService {
    /**
     * triggered when in the sidebar a copy / apply event was performed (mode SORT_INTO)
     */
    applyNodeEmitted = new EventEmitter<{ nodes: Node[]; parent?: Node }>();
    configChange$ = new EventEmitter<OptionConfig>();
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

    showOption(state: OptionState<OptionConfig>) {
        this._editorialSidebar.enabledOption.set(state);
        this.sidebarOpened.set(true);
    }

    patchOptionConfig(optionConfig: OptionConfig) {
        this.configChange$.emit(optionConfig);
        this._editorialSidebar.enabledOption.set({
            ...this._editorialSidebar.enabledOption(),
            optionConfig,
        });
    }
}
