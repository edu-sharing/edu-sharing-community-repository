import { Component, computed, inject, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule, IconDirective } from 'ngx-edu-sharing-ui';
import { Node } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { CollectionDetailsService } from '../../collection-details.service';
import { CollectionInfoRowComponent } from '../collection-info-row/collection-info-row.component';
import { CollectionInfoSectionComponent } from '../collection-info-section/collection-info-section.component';
import {
    collectionSortIdOf,
    SortId,
} from '../../../editorial-sidebar/create-collection-sidebar/collection-sorts';
import { TOPIC_PAGE_VIEW_TYPE } from '../../../topic-page/shared/utils/collection-topic-page-util';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { CollectionScopeLabelComponent } from '../../collection-scope-label/collection-scope-label.component';

/**
 * The facts of a collection in three sections: the numbers (content counts and views), the general
 * information (visibility, type) and the display settings (default view, sorting).
 */
@Component({
    selector: 'es-collection-info-facts',
    templateUrl: 'collection-info-facts.component.html',
    styleUrls: ['collection-info-facts.component.scss'],
    imports: [
        TranslateModule,
        EduSharingUiCommonModule,
        IconDirective,
        CollectionInfoSectionComponent,
        CollectionInfoRowComponent,
        CollectionScopeLabelComponent,
    ],
})
export class CollectionInfoFactsComponent {
    protected readonly details = inject(CollectionDetailsService);
    private readonly nodeHelper = inject(NodeHelperService);

    readonly collection = input.required<Node>();
    /** whether the default view and sorting are shown, which only those who may change them see */
    readonly showSettings = input(false);

    /** icon of the visibility, shown as the leading icon of its row */
    protected readonly visibilityIcon = computed(() => {
        const collection = this.collection();
        return (collection && this.nodeHelper.getCollectionScopeInfo(collection)?.icon) || 'lock';
    });
    protected readonly defaultViewKey = computed(() =>
        this.collection()?.collection?.viewtype === TOPIC_PAGE_VIEW_TYPE
            ? 'COLLECTIONS.INFO.VIEW_TOPIC_PAGE'
            : 'COLLECTIONS.INFO.VIEW_COLLECTION',
    );
    protected readonly subcollectionSortKey = computed(() =>
        this.sortKey('subcollections', RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE),
    );
    protected readonly referenceSortKey = computed(() =>
        this.sortKey('references', RestConstants.CCM_PROP_COLLECTION_ORDER_MODE),
    );

    /** translation key of the collection type; unknown types are shown as a plain collection */
    protected readonly typeKey = computed(() => {
        const type = this.collection()?.collection?.type;
        const known = [
            RestConstants.COLLECTIONTYPE_EDITORIAL,
            RestConstants.COLLECTIONTYPE_CURRICULUM,
            RestConstants.COLLECTIONTYPE_MEDIA_CENTER,
        ];
        return 'COLLECTIONS.INFO.TYPE.' + (known.includes(type) ? type : 'DEFAULT');
    });

    private sortKey(kind: 'subcollections' | 'references', property: string): string | null {
        const id: SortId | null = collectionSortIdOf(
            kind,
            this.collection()?.properties?.[property],
        );
        return id ? 'EDITORIAL.SIDEBAR.CREATE_COLLECTION.SORT.' + id : null;
    }
}
