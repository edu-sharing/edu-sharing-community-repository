import { EventEmitter, Injectable, signal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import {
    EditorialSidebarComponent,
    MULTISELECT_OPTIONS,
    OptionConfig,
    OptionState,
} from './editorial-sidebar.component';
import {
    NodeClickEvent,
    NodeEntriesDataType,
    NodeEntriesWrapperComponent,
} from 'ngx-edu-sharing-ui';
import { SelectionChange } from '@angular/cdk/collections';

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
    /**
     * indicate that the sidebar should overlay a global progress spinner
     */
    readonly sidebarLoading = signal(false);
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

    close() {
        this._editorialSidebar.enabledOption.set(null);
        this.sidebarOpened.set(false);
    }

    /**
     * handle a s select event from the node entries wrapper component and trigger the sidebar
     */
    handleSelect(
        nodeEntriesRef: NodeEntriesWrapperComponent<NodeEntriesDataType>,
        event: NodeClickEvent<NodeEntriesDataType>,
    ) {
        if (
            !(
                nodeEntriesRef?.getSelection()?.selected.length === 1 &&
                nodeEntriesRef?.getSelection()?.selected[0] === event.element
            )
        ) {
            nodeEntriesRef?.getSelection()?.clear();
            this.sidebarOpened.set(false);
        }
        nodeEntriesRef?.getSelection()?.toggle(event.element as Node);
        if (nodeEntriesRef?.getSelection()?.selected.length === 0) {
            this.sidebarOpened.set(false);
        } else if (
            nodeEntriesRef?.getSelection()?.selected.length === 1 &&
            (event.element as Node).mediatype &&
            !['collection', 'folder'].includes((event.element as Node).mediatype)
        ) {
            this.showOption({
                option: 'PREVIEW',
                trap: false,
            });
        }
    }

    handleSelection(selection: SelectionChange<NodeEntriesDataType>) {
        if (selection.source.selected.length === 0) {
            this.close();
        } else if (
            selection.source.selected.length !== 1 &&
            !MULTISELECT_OPTIONS.includes(this._editorialSidebar.enabledOption()?.option)
        ) {
            this.close();
        } else {
            //this.selection.set(selection.source.
        }
    }
}
