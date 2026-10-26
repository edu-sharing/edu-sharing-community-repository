import { Component, computed, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { Node } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { CollectionInfoRowComponent } from '../collection-info-row/collection-info-row.component';
import { CollectionInfoSectionComponent } from '../collection-info-section/collection-info-section.component';
import {
    collectionSortIdOf,
    SortId,
} from '../../../editorial-sidebar/create-collection-sidebar/collection-sorts';
import { TOPIC_PAGE_VIEW_TYPE } from '../../../topic-page/shared/utils/collection-topic-page-util';
import { CollectionScopeLabelComponent } from '../../collection-scope-label/collection-scope-label.component';

/** Visibility, type, content counts and creation/modification dates of a collection. */
@Component({
    selector: 'es-collection-info-facts',
    templateUrl: 'collection-info-facts.component.html',
    styleUrls: ['collection-info-facts.component.scss'],
    imports: [
        TranslateModule,
        EduSharingUiCommonModule,
        CollectionInfoSectionComponent,
        CollectionInfoRowComponent,
        CollectionScopeLabelComponent,
    ],
})
export class CollectionInfoFactsComponent {
    readonly collection = input.required<Node>();
    /** whether the default view and sorting are shown, which only those who may change them see */
    readonly showSettings = input(false);

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
