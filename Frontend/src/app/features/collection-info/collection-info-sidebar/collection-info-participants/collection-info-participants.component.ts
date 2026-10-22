import { Component, inject, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, UserAvatarComponent } from 'ngx-edu-sharing-ui';
import { CollectionInfoRowComponent } from '../collection-info-row/collection-info-row.component';
import { CollectionInfoSectionComponent } from '../collection-info-section/collection-info-section.component';
import { CollectionDetailsService } from '../../collection-details.service';

/** The creator of a collection, the authorities it is shared with and how often it was viewed. */
@Component({
    selector: 'es-collection-info-participants',
    templateUrl: 'collection-info-participants.component.html',
    styleUrls: ['collection-info-participants.component.scss'],
    imports: [
        TranslateModule,
        EduSharingUiCommonModule,
        CollectionInfoSectionComponent,
        CollectionInfoRowComponent,
        UserAvatarComponent,
    ],
})
export class CollectionInfoParticipantsComponent {
    protected readonly details = inject(CollectionDetailsService);

    readonly collection = input.required<Node>();
}
