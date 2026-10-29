import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { RestHelper } from '../../core-module/rest/rest-helper';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';

/** Navigation targets that belong to a single collection. */
@Injectable({ providedIn: 'root' })
export class CollectionNavigationService {
    private readonly router = inject(Router);
    private readonly editorialSidebarService = inject(EditorialSidebarService);

    /** Whether the current user may open the collection in the editor. */
    canEdit(collection: Node): boolean {
        return RestHelper.hasAccessPermission(collection, RestConstants.PERMISSION_WRITE);
    }

    /** Whether the page has a sidebar that can show the information about the collection. */
    canShowInfo(): boolean {
        return !!this.editorialSidebarService.editorialSidebar;
    }

    /** Opens the information about the collection in the sidebar of the page. */
    showInfo(): void {
        this.editorialSidebarService.showOption({ option: 'COLLECTION_INFO', trap: false });
    }

    /**
     * Opens the editor of the collection: in the sidebar of the page if it has one, else as a page
     * of its own that keeps the `mainnav` setting of the current page.
     */
    edit(collection: Node): Promise<boolean> {
        if (this.editorialSidebarService.editorialSidebar) {
            const sidebar = this.editorialSidebarService.editorialSidebar;
            // the sidebar describes the collection that is selected, else its parent
            if (sidebar.collectionInfoTarget()?.ref.id !== collection.ref.id) {
                this.editorialSidebarService.nodes.set([collection]);
            }
            this.editorialSidebarService.showOption({
                option: 'EDIT_COLLECTION_MENU',
                trap: false,
            });
            return Promise.resolve(true);
        }
        const mainnav = this.router.parseUrl(this.router.url).queryParams['mainnav'];
        return this.router.navigate(
            [UIConstants.ROUTER_PREFIX + 'collections/collection', 'edit', collection.ref.id],
            { queryParams: { mainnav: mainnav !== 'false' } },
        );
    }
}
