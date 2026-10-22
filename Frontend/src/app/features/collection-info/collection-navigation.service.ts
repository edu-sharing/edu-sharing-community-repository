import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { RestHelper } from '../../core-module/rest/rest-helper';

/** Navigation targets that belong to a single collection. */
@Injectable({ providedIn: 'root' })
export class CollectionNavigationService {
    private readonly router = inject(Router);

    /** Whether the current user may open the collection in the editor. */
    canEdit(collection: Node): boolean {
        return RestHelper.hasAccessPermission(collection, RestConstants.PERMISSION_WRITE);
    }

    /** Opens the editor of the collection, keeping the `mainnav` setting of the current page. */
    edit(collection: Node): Promise<boolean> {
        const mainnav = this.router.parseUrl(this.router.url).queryParams['mainnav'];
        return this.router.navigate(
            [UIConstants.ROUTER_PREFIX + 'collections/collection', 'edit', collection.ref.id],
            { queryParams: { mainnav: mainnav !== 'false' } },
        );
    }
}
