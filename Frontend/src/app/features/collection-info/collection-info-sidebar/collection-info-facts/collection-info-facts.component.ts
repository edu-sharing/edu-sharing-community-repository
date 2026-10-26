import { Component, computed, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { CollectionInfoRowComponent } from '../collection-info-row/collection-info-row.component';
import { CollectionInfoSectionComponent } from '../collection-info-section/collection-info-section.component';
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
}
