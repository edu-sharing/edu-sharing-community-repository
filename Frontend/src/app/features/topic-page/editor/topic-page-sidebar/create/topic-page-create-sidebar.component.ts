import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../../../shared/shared.module';
import { EditorialSidebarService } from '../../../../editorial-sidebar/editorial-sidebar.service';
import { SidebarStickyFooterComponent } from '../../../../editorial-sidebar/sidebar-sticky-footer/sidebar-sticky-footer.component';
import { CreationKind } from '../../../shared/types/variant-preview';
import {
    CopyOption,
    PageVariantOrTemplatePickerComponent,
    PageVariantSearchState,
} from '../../page-variant-or-template-picker/page-variant-or-template-picker.component';
import {
    TopicPageSidebarHost,
    TopicPageSidebarService,
    TopicPageSidebarTab,
} from '../topic-page-sidebar.service';

/** what is picked on a creation tab, kept per tab while switching between them */
type CreationState = {
    node: Node | null;
    copyOption: CopyOption | null;
    searchState: PageVariantSearchState | null;
};

const TAB_ICONS: Record<TopicPageSidebarTab, string> = {
    variant: 'edu-page_variant',
    template: 'edu-add_page_template',
};

/**
 * Entry of the topic page's sidebar menu that creates a page variant or template from an existing
 * one, on a tab each while both can be created.
 */
@Component({
    selector: 'es-topic-page-create-sidebar',
    imports: [SharedModule, PageVariantOrTemplatePickerComponent, SidebarStickyFooterComponent],
    templateUrl: './topic-page-create-sidebar.component.html',
    styleUrls: ['./topic-page-create-sidebar.component.scss'],
})
export class TopicPageCreateSidebarComponent {
    private readonly editorialSidebarService = inject(EditorialSidebarService);
    private readonly topicPageSidebarService = inject(TopicPageSidebarService);

    readonly i18nPrefix: string = 'TOPIC_PAGE.SIDEBAR.';
    readonly TAB_ICONS = TAB_ICONS;
    readonly host: TopicPageSidebarHost = this.topicPageSidebarService.host;
    readonly tabs = this.host.creationKinds;
    // the tab the option was opened with, as long as the user picked no other one
    readonly selectedTab = linkedSignal<TopicPageSidebarTab>(() => {
        const requested: TopicPageSidebarTab | undefined =
            this.topicPageSidebarService.config()?.tab;
        return this.tabs().includes(requested) ? requested : this.tabs()[0];
    });
    readonly selectedIndex = computed(() => this.tabs().indexOf(this.selectedTab()));
    // the inputs of the pickers are read once they are created, so they take the state of opening
    readonly pageConfigRef: string | null = this.host.pageConfigRef();
    readonly pageTemplateExists: boolean = this.host.pageTemplateExists();
    readonly restoredSearchStates: Record<CreationKind, PageVariantSearchState | null> = {
        variant: this.host.restoredSearchState('variant'),
        template: this.host.restoredSearchState('template'),
    };
    private readonly states = signal<Partial<Record<CreationKind, CreationState>>>({});
    readonly selectedNode = computed(() => this.states()[this.selectedTab()]?.node ?? null);
    readonly busy = signal(false);

    // workaround: the shared picker `ng-template` passes `kind` untyped, which a template cannot
    // use as a typed `Record` key; the parameter gives it the `CreationKind` type
    restoredSearchState(kind: CreationKind): PageVariantSearchState | null {
        return this.restoredSearchStates[kind];
    }

    onTabChange(index: number): void {
        this.selectedTab.set(this.tabs()[index]);
    }

    patchState(kind: CreationKind, patch: Partial<CreationState>): void {
        this.states.update((states) => ({
            ...states,
            [kind]: { node: null, copyOption: null, searchState: null, ...states[kind], ...patch },
        }));
    }

    cancel(): void {
        this.editorialSidebarService.close();
    }

    async preview(): Promise<void> {
        const kind: CreationKind = this.selectedTab();
        const state: CreationState | undefined = this.states()[kind];
        if (!state?.node) {
            return;
        }
        await this.run(() =>
            this.host.preview(kind, state.copyOption, state.searchState, state.node),
        );
    }

    async use(): Promise<void> {
        const kind: CreationKind = this.selectedTab();
        const state: CreationState | undefined = this.states()[kind];
        if (!state?.node) {
            return;
        }
        await this.run(() => this.host.use(kind, state.node, state.copyOption));
    }

    private async run(action: () => Promise<void>): Promise<void> {
        this.busy.set(true);
        try {
            await action();
        } finally {
            this.busy.set(false);
        }
    }
}
