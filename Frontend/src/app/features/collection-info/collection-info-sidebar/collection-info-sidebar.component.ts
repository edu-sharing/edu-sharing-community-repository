import { Component, effect, inject, input, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    EduSharingUiCommonModule,
    OptionItemToggle,
    OptionsHelperDataService,
    Scope,
} from 'ngx-edu-sharing-ui';
import { CollectionInfoSectionComponent } from './collection-info-section/collection-info-section.component';
import { SidebarStickyFooterComponent } from '../../editorial-sidebar/sidebar-sticky-footer/sidebar-sticky-footer.component';
import { MdsEditorInstanceService } from '../../mds/mds-editor/mds-editor-instance.service';
import { CollectionDetailsService } from '../collection-details.service';
import { CollectionNavigationService } from '../collection-navigation.service';
import { CollectionInfoFactsComponent } from './collection-info-facts/collection-info-facts.component';
import { CollectionInfoHeroComponent } from './collection-info-hero/collection-info-hero.component';
import { CollectionInfoParticipantsComponent } from './collection-info-participants/collection-info-participants.component';

/** names of the options that are not offered next to the collection information */
const HIDDEN_OPTIONS = ['EDITORIAL.OPTIONS.COLLECTION_INFO', 'OPTIONS.SHOW_TOPIC_PAGE'];
/** names of the options that get a button of their own, all others are listed in the menu */
const BUTTON_OPTIONS = ['OPTIONS.INVITE', 'OPTIONS.QR_CODE'];

/**
 * Sidebar page with the general information about a collection. It only arranges the sections;
 * each section renders one aspect of the collection on its own.
 */
@Component({
    selector: 'es-collection-info-sidebar',
    templateUrl: 'collection-info-sidebar.component.html',
    styleUrls: ['collection-info-sidebar.component.scss'],
    providers: [MdsEditorInstanceService, CollectionDetailsService, OptionsHelperDataService],
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
    private readonly optionsHelper = inject(OptionsHelperDataService);
    protected readonly mdsEditorInstanceService = inject(MdsEditorInstanceService);

    readonly collection = input.required<Node>();
    /** metadata group that lists the properties shown in the metadata section */
    readonly metadataGroupId = input('collection_sidebar');

    /** the actionbar with the actions of the collection */
    private readonly actionbar = viewChild(ActionbarComponent);

    constructor() {
        const details = inject(CollectionDetailsService);
        effect(() => details.collection.set(this.collection()));
        details.activate();
        effect(() => {
            const actionbar = this.actionbar();
            const collection = this.collection();
            if (actionbar) {
                void this.updateOptions(actionbar, collection);
            }
        });
    }

    protected canEdit(): boolean {
        return this.navigation.canEdit(this.collection());
    }

    protected showAllDetails(): void {
        void this.navigation.edit(this.collection());
    }

    /**
     * Binds the actions of the collection to the actionbar. The toggles of the list and the
     * actions that only lead to another view of the collection (this sidebar, the topic page) are
     * left out, so that the editing actions come first.
     */
    private async updateOptions(actionbar: ActionbarComponent, collection: Node): Promise<void> {
        await this.optionsHelper.initComponents(actionbar);
        this.optionsHelper.setData({
            scope: Scope.CollectionsCollection,
            activeObjects: [collection],
            postPrepareOptions: (options) => {
                for (const option of options) {
                    option.showAsAction = BUTTON_OPTIONS.includes(option.name);
                }
                options.splice(
                    0,
                    options.length,
                    ...options.filter(
                        (o) =>
                            !(o as OptionItemToggle).isToggle && !HIDDEN_OPTIONS.includes(o.name),
                    ),
                );
            },
        });
        await this.optionsHelper.refreshComponents();
    }
}
