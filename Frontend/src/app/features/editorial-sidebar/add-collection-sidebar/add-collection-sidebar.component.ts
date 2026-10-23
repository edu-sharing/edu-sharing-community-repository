import { Component, inject, input } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../shared/shared.module';
import {
    NodesSelectorConfig,
    TabType,
} from '../../../pages/editorial-page/nodes-selector/nodes-selector.component';
import { CreateCollectionConfig } from '../create-collection-sidebar/create-collection-sidebar.component';
import { EditorialSidebarService } from '../editorial-sidebar.service';

type AddCollectionChoice = {
    /** id of the choice, also used as the i18n sub key */
    id: 'CREATE' | 'COPY';
    icon: string;
    action: () => void;
};

/**
 * Entry step of the "add collection" flow: lets the user decide whether a new collection should be
 * created or an existing one copied to the current location.
 */
@Component({
    selector: 'es-add-collection-sidebar',
    templateUrl: 'add-collection-sidebar.component.html',
    styleUrls: ['add-collection-sidebar.component.scss'],
    imports: [SharedModule],
})
export class AddCollectionSidebarComponent {
    private editorialSidebarService = inject(EditorialSidebarService);

    /** the collection the new/copied collection is added to (unset on the collections root) */
    parent = input<Node>();

    protected readonly i18nPrefix = 'EDITORIAL.SIDEBAR.ADD_COLLECTION.';
    protected readonly choices: AddCollectionChoice[] = [
        { id: 'CREATE', icon: 'library_add', action: () => this.create() },
        { id: 'COPY', icon: 'file_copy', action: () => this.copy() },
    ];

    /** Opens the form for a new collection below the current parent. */
    private create(): void {
        this.editorialSidebarService.showOption({
            option: 'CREATE_COLLECTION',
            trap: true,
            optionConfig: { entry: 'add' } as CreateCollectionConfig,
        });
    }

    /**
     * Hands over to the nodes-selector, restricted to the collections view, so an existing
     * collection can be picked and copied into the current parent.
     */
    private copy(): void {
        this.editorialSidebarService.showOption({
            option: 'SORT_INTO',
            trap: false,
            title: this.i18nPrefix + 'COPY.TITLE',
            optionConfig: {
                state: TabType.COLLECTIONS,
                // the generic "copy selected content" label does not fit this flow
                applyLabel: this.i18nPrefix + 'TITLE',
                // the flow ends with the copy, so close the sidebar afterwards
                autoClose: true,
                // an existing collection is the source here, and the parent is its copy target
                allowCollectionSelection: true,
                // only existing collections may be picked as the source
                tabBlacklist: [
                    TabType.SEARCH,
                    TabType.METHODOLOGY,
                    TabType.WORKSPACE,
                    TabType.UPLOAD,
                ],
            } as NodesSelectorConfig,
        });
    }
}
