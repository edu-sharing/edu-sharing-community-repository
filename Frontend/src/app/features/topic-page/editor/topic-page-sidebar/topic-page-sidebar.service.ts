import { inject, Injectable, Signal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { OptionConfig } from '../../../editorial-sidebar/editorial-sidebar.component';
import { EditorialSidebarService } from '../../../editorial-sidebar/editorial-sidebar.service';
import { TopicPageEntry } from '../../shared/services/topic-page-manage.service';
import { CreationKind } from '../../shared/types/variant-preview';
import {
    CopyOption,
    PageVariantSearchState,
} from '../page-variant-or-template-picker/page-variant-or-template-picker.component';

/** id of the topic page's menu in the editorial sidebar, listing the options below */
export const TOPIC_PAGE_SIDEBAR_OPTION = 'TOPIC_PAGE';
/** lists the pages and templates of the collection to show, regenerate or delete them */
export const TOPIC_PAGE_MANAGE_OPTION = 'TOPIC_PAGE_MANAGE';
/** creates a page variant or template from an existing one */
export const TOPIC_PAGE_CREATE_OPTION = 'TOPIC_PAGE_CREATE';
/** passes pages on to sub-collections */
export const TOPIC_PAGE_INHERITANCE_OPTION = 'TOPIC_PAGE_INHERITANCE';
/** the menu first, its entries in the order they are offered */
export const TOPIC_PAGE_SIDEBAR_OPTIONS: readonly string[] = [
    TOPIC_PAGE_SIDEBAR_OPTION,
    TOPIC_PAGE_MANAGE_OPTION,
    TOPIC_PAGE_CREATE_OPTION,
    TOPIC_PAGE_INHERITANCE_OPTION,
];

export type TopicPageSidebarTab = CreationKind;

/** label of the creation entry, naming templates too while they can be created */
export function createOptionLabel(kinds: CreationKind[]): string {
    return kinds.includes('template')
        ? 'TOPIC_PAGE.SIDEBAR.MENU.CREATE_WITH_TEMPLATE'
        : 'TOPIC_PAGE.SIDEBAR.MENU.CREATE';
}

export type TopicPageSidebarConfig = {
    /** the creation tab to show; falls back to the first one available */
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
    // whether the collection already passes page templates on to its sub-collections
    pageTemplateExists(): boolean;
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
    // whether the collection's templates are shown on the page instead of its variants
    templateMode: Signal<boolean>;
    // whether the user may edit the collection's templates
    canEditTemplates: Signal<boolean>;
    // whether the pages of the collection can be acted on right now
    canManage: Signal<boolean>;
    showEntry(entry: TopicPageEntry): Promise<void>;
    // shows the entry and rebuilds it from its template, after asking
    regenerateEntry(entry: TopicPageEntry): Promise<void>;
    deleteEntry(entry: TopicPageEntry): Promise<void>;
    // opens the template the entry was built from in the editor
    editTemplate(entry: TopicPageEntry): Promise<void>;
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
     * Opens the creation on the given tab, with a way back to the menu.
     */
    open(tab: TopicPageSidebarTab): void {
        const optionConfig: TopicPageSidebarConfig = { tab };
        this.editorialSidebarService.showOption({
            option: TOPIC_PAGE_CREATE_OPTION,
            trap: false,
            optionConfig: optionConfig as OptionConfig,
            backTo: { option: TOPIC_PAGE_SIDEBAR_OPTION, trap: false },
        });
    }

    /**
     * Opens the list of the collection's pages and templates.
     */
    openManage(trap: boolean = false): void {
        this.editorialSidebarService.showOption({
            option: TOPIC_PAGE_MANAGE_OPTION,
            trap,
            backTo: { option: TOPIC_PAGE_SIDEBAR_OPTION, trap: false },
        });
    }

    /**
     * Opens the topic menu if the sidebar shows no option, the menu is registered and no content
     * is selected, which the menu does not act on.
     */
    openMenuIfIdle(): void {
        const sidebar = this.editorialSidebarService.editorialSidebar;
        const registered = this.editorialSidebarService
            .getCustomOptions()
            .some((custom) => custom.id === TOPIC_PAGE_SIDEBAR_OPTION);
        if (
            !sidebar ||
            sidebar.enabledOption() ||
            !registered ||
            this.editorialSidebarService.nodes()?.length
        ) {
            return;
        }
        this.editorialSidebarService.showOption({ option: TOPIC_PAGE_SIDEBAR_OPTION, trap: false });
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
        return TOPIC_PAGE_SIDEBAR_OPTIONS.includes(
            this.editorialSidebarService.editorialSidebar?.enabledOption()?.option,
        );
    }

    /** the config the creation was opened with */
    config(): TopicPageSidebarConfig | null {
        if (
            this.editorialSidebarService.editorialSidebar?.enabledOption()?.option !==
            TOPIC_PAGE_CREATE_OPTION
        ) {
            return null;
        }
        return this.editorialSidebarService.editorialSidebar.enabledOption()
            .optionConfig as TopicPageSidebarConfig;
    }
}
