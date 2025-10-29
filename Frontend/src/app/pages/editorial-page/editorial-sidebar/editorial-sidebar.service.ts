import { Injectable, signal } from '@angular/core';
import { EditorialSidebarComponent, OptionState } from './editorial-sidebar.component';

@Injectable({
    providedIn: 'root',
})
export class EditorialSidebarService {
    private _editorialSidebar: EditorialSidebarComponent;
    readonly sidebarOpened = signal(false);
    registerSidebar(editorialSidebar: EditorialSidebarComponent) {
        if (this._editorialSidebar) {
            console.error(
                'Duplicate registration of editorial sidebar',
                this._editorialSidebar,
                editorialSidebar,
            );
            throw new Error('Duplicate registration of editorial sidebar');
        }
        this._editorialSidebar = editorialSidebar;
    }

    get editorialSidebar(): EditorialSidebarComponent {
        return this._editorialSidebar;
    }

    showOption(state: OptionState) {
        this._editorialSidebar.enabledOption.set(state);
        this.sidebarOpened.set(true);
    }
}
