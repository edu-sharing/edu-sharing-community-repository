import { inject, Injectable, InjectionToken, Signal } from '@angular/core';
import { MdsWidget, Node } from 'ngx-edu-sharing-api';
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
/** edits the title and parameters of the page or template shown in the editor */
export const TOPIC_PAGE_SETTINGS_OPTION = 'TOPIC_PAGE_SETTINGS';
/** lists the pages and templates of the collection to show, regenerate, publish or delete them */
export const TOPIC_PAGE_MANAGE_OPTION = 'TOPIC_PAGE_MANAGE';
/** creates a page variant or template from an existing one */
export const TOPIC_PAGE_CREATE_OPTION = 'TOPIC_PAGE_CREATE';
/** passes pages on to sub-collections */
export const TOPIC_PAGE_INHERITANCE_OPTION = 'TOPIC_PAGE_INHERITANCE';
/** the menu first, its entries in the order they are offered */
export const TOPIC_PAGE_SIDEBAR_OPTIONS: readonly string[] = [
    TOPIC_PAGE_SIDEBAR_OPTION,
    TOPIC_PAGE_SETTINGS_OPTION,
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

/** the page, template or global template the settings entry edits, as shown in the editor */
export type TopicPageSettings = {
    node: Node;
    // the title the form starts with; untitled pages are named by their position
    title: string;
    // the other pages of the same config, which must not have the same parameters
    configNodes: Node[];
    // the parameters that tell the pages of a config apart
    dimensions: Map<string, MdsWidget>;
    templateMode: boolean;
    globalTemplateMode: boolean;
    canDeleteGlobalTemplate: boolean;
};

/**
 * The topic page as the sidebar option acts on it, provided by the topic page editor.
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
    // opens the templates of the given collection in its template mode
    editInheritedTemplates(source: Node): Promise<void>;
    // what the settings entry edits, null while nothing can be configured
    settings(): TopicPageSettings | null;
    applySettings(changes: Map<string, string | string[]>): Promise<void>;
    // whether the user may publish pages as global templates
    canAddToGlobalTemplates: Signal<boolean>;
    // publishes the shown page as a global template, with the given unsaved settings
    addToGlobalTemplates(changes: Map<string, string | string[]>): Promise<void>;
    // shows the entry and publishes it as a global template
    addEntryToGlobalTemplates(entry: TopicPageEntry): Promise<void>;
    // whether every parameter that tells the pages apart is set on the node
    parametersComplete(node: Node): boolean;
    // deletes the global template shown in the editor
    deleteGlobalTemplate(): Promise<void>;
}

/** the page the entries of the topic page's sidebar menu act on */
export const TOPIC_PAGE_SIDEBAR_HOST = new InjectionToken<TopicPageSidebarHost>(
    'TOPIC_PAGE_SIDEBAR_HOST',
);

/**
 * Opens and closes the entries of the topic page's menu in the editorial sidebar.
 */
@Injectable()
export class TopicPageSidebarService {
    private editorialSidebarService = inject(EditorialSidebarService);

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
     * Opens the settings of the page or template shown in the editor.
     */
    openSettings(): void {
        this.editorialSidebarService.showOption({
            option: TOPIC_PAGE_SETTINGS_OPTION,
            trap: false,
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

    /**
     * Closes the sidebar if it shows the settings, which belong to the page shown before.
     */
    closeSettings(): void {
        if (
            this.editorialSidebarService.editorialSidebar?.enabledOption()?.option ===
            TOPIC_PAGE_SETTINGS_OPTION
        ) {
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
