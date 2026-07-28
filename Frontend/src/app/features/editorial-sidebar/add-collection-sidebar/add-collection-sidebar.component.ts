import { Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { Node, ROOT } from 'ngx-edu-sharing-api';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../shared/shared.module';
import {
    NodesSelectorConfig,
    TabType,
} from '../../../pages/editorial-page/nodes-selector/nodes-selector.component';
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
    private router = inject(Router);

    /** the collection the new/copied collection is added to (unset on the collections root) */
    parent = input<Node>();

    protected readonly i18nPrefix = 'EDITORIAL.SIDEBAR.ADD_COLLECTION.';
    protected readonly choices: AddCollectionChoice[] = [
        { id: 'CREATE', icon: 'library_add', action: () => void this.create() },
        { id: 'COPY', icon: 'file_copy', action: () => this.copy() },
    ];

    /**
     * Opens the collection editor for a new collection below the current parent. The common url
     * parameters are kept by the `AppLocationStrategy`, so they must not be passed on here.
     */
    private async create(): Promise<void> {
        await this.router.navigate([
            UIConstants.ROUTER_PREFIX + 'collections/collection',
            'new',
            this.parent()?.ref?.id ?? ROOT,
        ]);
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
                // this is the only flow that may copy a whole collection
                allowCollectionCopy: true,
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
