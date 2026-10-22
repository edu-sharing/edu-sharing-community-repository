import { Component, effect, inject, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { CollectionInfoSectionComponent } from './collection-info-section/collection-info-section.component';
import { SidebarStickyFooterComponent } from '../../editorial-sidebar/sidebar-sticky-footer/sidebar-sticky-footer.component';
import { MdsEditorInstanceService } from '../../mds/mds-editor/mds-editor-instance.service';
import { CollectionDetailsService } from '../collection-details.service';
import { CollectionNavigationService } from '../collection-navigation.service';
import { CollectionInfoFactsComponent } from './collection-info-facts/collection-info-facts.component';
import { CollectionInfoHeroComponent } from './collection-info-hero/collection-info-hero.component';
import { CollectionInfoParticipantsComponent } from './collection-info-participants/collection-info-participants.component';

/**
 * Sidebar page with the general information about a collection. It only arranges the sections;
 * each section renders one aspect of the collection on its own.
 */
@Component({
    selector: 'es-collection-info-sidebar',
    templateUrl: 'collection-info-sidebar.component.html',
    styleUrls: ['collection-info-sidebar.component.scss'],
    providers: [MdsEditorInstanceService, CollectionDetailsService],
    imports: [
        MatButtonModule,
        TranslateModule,
        CollectionInfoSectionComponent,
        EduSharingUiCommonModule,
        SidebarStickyFooterComponent,
        CollectionInfoHeroComponent,
        CollectionInfoFactsComponent,
        CollectionInfoParticipantsComponent,
    ],
})
export class CollectionInfoSidebarComponent {
    private readonly navigation = inject(CollectionNavigationService);
    protected readonly mdsEditorInstanceService = inject(MdsEditorInstanceService);

    readonly collection = input.required<Node>();
    /** metadata group that lists the properties shown in the metadata section */
    readonly metadataGroupId = input('collection_sidebar');

    constructor() {
        const details = inject(CollectionDetailsService);
        effect(() => details.collection.set(this.collection()));
        details.activate();
    }

    protected canEdit(): boolean {
        return this.navigation.canEdit(this.collection());
    }

    protected showAllDetails(): void {
        void this.navigation.edit(this.collection());
    }
}
