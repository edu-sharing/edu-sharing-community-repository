import { Component, DestroyRef, computed, inject, input, signal, viewChild } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { LocalEventsService } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../shared/shared.module';
import { ShareDialogData } from '../../dialogs/dialog-modules/share-dialog/share-dialog-data';
import { ShareDialogComponent } from '../../dialogs/dialog-modules/share-dialog/share-dialog.component';
import { ShareDialogModule } from '../../dialogs/dialog-modules/share-dialog/share-dialog.module';
import { EditorialSidebarService } from '../editorial-sidebar.service';
import { SidebarStickyFooterComponent } from '../sidebar-sticky-footer/sidebar-sticky-footer.component';

/** Gives others access to a collection, with the share dialog embedded in the sidebar. */
@Component({
    selector: 'es-share-collection-sidebar',
    templateUrl: 'share-collection-sidebar.component.html',
    styleUrls: ['share-collection-sidebar.component.scss'],
    imports: [SharedModule, ShareDialogModule, SidebarStickyFooterComponent],
})
export class ShareCollectionSidebarComponent {
    private readonly editorialSidebarService = inject(EditorialSidebarService);
    private readonly localEvents = inject(LocalEventsService);

    readonly collection = input.required<Node>();

    protected readonly saving = signal(false);
    private readonly shareDialog = viewChild(ShareDialogComponent);
    protected readonly shareConfig = computed<ShareDialogData>(() => ({
        nodes: [this.collection()],
        sendMessages: true,
        sendToApi: true,
    }));

    constructor() {
        this.editorialSidebarService.titleOverride.set('COLLECTIONS.EDIT_PERMISSIONS');
        inject(DestroyRef).onDestroy(() => this.editorialSidebarService.titleOverride.set(null));
    }

    /** Leaves the page the same way the back arrow of the sidebar header does. */
    protected cancel(): void {
        void this.editorialSidebarService.editorialSidebar?.goBack();
    }

    protected async save(): Promise<void> {
        const id = this.collection().ref.id;
        let saved = false;
        const subscription = this.localEvents.nodesChanged.subscribe((nodes) => {
            saved ||= nodes.some((node) => node?.ref?.id === id);
        });
        this.saving.set(true);
        try {
            await this.shareDialog().save();
        } finally {
            subscription.unsubscribe();
            this.saving.set(false);
        }
        if (saved) {
            this.editorialSidebarService.close();
        }
    }
}
