import { inject, Injectable, Signal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { OptionConfig } from '../../../editorial-sidebar/editorial-sidebar.component';
import { EditorialSidebarService } from '../../../editorial-sidebar/editorial-sidebar.service';
import { CreationKind } from '../../shared/types/variant-preview';
import {
    CopyOption,
    PageVariantSearchState,
} from '../page-variant-or-template-picker/page-variant-or-template-picker.component';

/** id of the topic page's option in the editorial sidebar */
export const TOPIC_PAGE_SIDEBAR_OPTION = 'TOPIC_PAGE';

export type TopicPageSidebarTab = CreationKind;

export type TopicPageSidebarConfig = {
    /** the tab to show; falls back to the first one available */
    tab?: TopicPageSidebarTab;
};

/**
 * The topic page as the sidebar option acts on it. Implemented by the topic page editor.
 */
export interface TopicPageSidebarHost {
    // the collection the topic page belongs to
    collectionNodeId: Signal<string>;
    // the collection's own page variant, also while a preview is shown in its place
    pageVariantNode: Signal<Node | null>;
    // the page config of the collection, if it has one
    pageConfigRef(): string | null;
    // what can be created on the page right now, in the order of the tabs
    creationKinds: Signal<CreationKind[]>;
    // the search of a running preview of the given kind, to continue with
    restoredSearchState(kind: CreationKind): PageVariantSearchState | null;
    preview(
        kind: CreationKind,
        copyOption: CopyOption,
        searchState: PageVariantSearchState | null,
        node: Node,
    ): Promise<void>;
    use(kind: CreationKind, node: Node, copyOption: CopyOption): Promise<void>;
    editGlobalTemplate(node: Node): Promise<void>;
    globalTemplateDeleted(node: Node): Promise<void>;
}

/**
 * Connects the topic page's option in the editorial sidebar with the page it belongs to.
 */
@Injectable()
export class TopicPageSidebarService {
    private editorialSidebarService = inject(EditorialSidebarService);

    private _host: TopicPageSidebarHost | null = null;

    get host(): TopicPageSidebarHost | null {
        return this._host;
    }

    attach(host: TopicPageSidebarHost): void {
        this._host = host;
    }

    /**
     * Opens the option on the given tab. Trapped, it offers no way back to the option list.
     */
    open(tab: TopicPageSidebarTab, trap: boolean = true): void {
        const optionConfig: TopicPageSidebarConfig = { tab };
        this.editorialSidebarService.showOption({
            option: TOPIC_PAGE_SIDEBAR_OPTION,
            trap,
            optionConfig: optionConfig as OptionConfig,
        });
    }

    /**
     * Closes the sidebar if it shows the option.
     */
    close(): void {
        if (this.isOpen()) {
            this.editorialSidebarService.close();
        }
    }

    isOpen(): boolean {
        return (
            this.editorialSidebarService.editorialSidebar?.enabledOption()?.option ===
            TOPIC_PAGE_SIDEBAR_OPTION
        );
    }

    /** the config the option was opened with */
    config(): TopicPageSidebarConfig | null {
        if (!this.isOpen()) {
            return null;
        }
        return this.editorialSidebarService.editorialSidebar.enabledOption()
            .optionConfig as TopicPageSidebarConfig;
    }
}
