import { effect, EventEmitter, inject, Injectable, signal, Type, untracked } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import {
    EDITORIAL_SIDEBAR_OPTIONS,
    EditorialSidebarComponent,
    EditorialSidebarOption,
    EditorialSidebarOptionDescriptor,
    OptionConfig,
    OptionState,
    PreviewConfig,
    SelectionMode,
} from './editorial-sidebar.component';
import {
    ElementType,
    NodeClickEvent,
    OptionGroup,
    NodeEntriesDataType,
    NodeEntriesGlobalService,
    NodeEntriesWrapperComponent,
    Scope,
} from 'ngx-edu-sharing-ui';
import { SelectionChange } from '@angular/cdk/collections';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { distinctUntilChanged, map, skip } from 'rxjs/operators';

/**
 * An option another module contributes to the sidebar. The sidebar knows nothing about it beyond
 * this description: it offers it in the option list and renders `component` when it is opened, so
 * the implementation can live outside of the core (see `custom-global-extensions` of an extension).
 */
export type CustomSidebarOption = {
    /** identifies the option; also the value of `OptionState.option` while it is open */
    id: string;
    /** i18n key (or plain text) for the entry in the option list and the panel heading */
    label: string;
    icon: string;
    /** rendered while the option is open (via `ngComponentOutlet`, so it must be standalone) */
    component: Type<unknown>;
    /**
     * Defaults to `'any'`: a contributed option is usually about the current context rather than
     * about the selection, and would otherwise be dropped the moment something is selected.
     */
    selectionMode?: SelectionMode;
    /** `OptionItem` fields passed through to restrict where the entry is offered */
    group?: OptionGroup;
    scopes?: string[];
    /**
     * Which selections the option is offered for. Defaults to every type, i.e. independent of the
     * selection — unlike `OptionItem`, which defaults to `[ElementType.Node]` and would hide the
     * entry whenever nothing (or something else) is selected.
     */
    elementType?: ElementType[];
};

@Injectable({
    providedIn: 'root',
})
export class EditorialSidebarService {
    mainNavService = inject(MainNavService);
    private nodeEntriesGlobalService = inject(NodeEntriesGlobalService);

    private readonly customOptions = signal<CustomSidebarOption[]>([]);

    /**
     * Contribute an option to the sidebar. Registering the same id twice replaces the previous
     * registration rather than offering the option twice.
     */
    registerCustomOption(option: CustomSidebarOption) {
        this.customOptions.update((options) => [
            ...options.filter((existing) => existing.id !== option.id),
            option,
        ]);
    }

    unregisterCustomOption(id: string) {
        this.customOptions.update((options) => options.filter((existing) => existing.id !== id));
    }

    getCustomOptions(): CustomSidebarOption[] {
        return this.customOptions();
    }

    getCustomOption(id: EditorialSidebarOption): CustomSidebarOption | null {
        return this.customOptions().find((option) => option.id === id) ?? null;
    }

    /**
     * How the given option behaves — from the sidebar's own table, else from the registration.
     */
    getOptionDescriptor(option: EditorialSidebarOption): EditorialSidebarOptionDescriptor {
        const builtIn = (
            EDITORIAL_SIDEBAR_OPTIONS as Record<string, EditorialSidebarOptionDescriptor>
        )[option];
        if (builtIn) {
            return builtIn;
        }
        const custom = this.getCustomOption(option);
        return { selectionMode: custom?.selectionMode ?? 'any' };
    }

    /**
     * currently selected nodes
     * (handled via handleSelection() and used by the component for displaying the item)
     */
    nodes = signal<NodeEntriesDataType[]>(null);
    /**
     * triggered when in the sidebar a copy / apply event was performed (mode SORT_INTO)
     */
    applyNodeEmitted = new EventEmitter<{
        nodes: Node[];
        parent?: Node;
        // when a connector is used, otherwise null
        connectorId?: string;
        // only non-null when a connector is used
        window?: Window;
        // true when the nodes were newly created (upload/link/connector) rather than existing
        // nodes being included; hosts use it to select+show-options vs. just add+keep-open
        created?: boolean;
    }>();
    configChange$ = new EventEmitter<OptionConfig>();
    scope = signal(Scope.Search);
    private _editorialSidebar: EditorialSidebarComponent;
    /**
     * Whether a sidebar is present on the current page, i.e. whether an option can be shown at all.
     * A signal, so hosts outside the page can react to it (the plain `editorialSidebar` getter
     * cannot be observed).
     */
    readonly sidebarAvailable = signal(false);
    readonly sidebarOpened = signal(false);
    /**
     * indicate that the sidebar should overlay a global progress spinner
     */
    readonly sidebarLoading = signal(false);
    /**
     * whether the sidebar is expanded to fullscreen. Reset whenever an option is
     * newly opened or closed (see EditorialSidebarComponent).
     */
    readonly fullscreenActive = signal(false);
    /**
     * whether the fullscreen toggle should be offered. Set by the preview content once
     * it renders a node; reset whenever an option is newly opened or closed.
     */
    readonly showFullscreenToggle = signal(false);

    toggleFullscreen() {
        this.fullscreenActive.update((v) => !v);
    }

    constructor() {
        this.mainNavService
            .observeMainNavConfig()
            .pipe(
                map((config) => config.currentScope),
                distinctUntilChanged(),
                skip(1),
            )
            .subscribe(() => this.close());

        // Re-opening the sidebar manually (toggle) only flips `sidebarOpened`; a prior close()
        // cleared `nodes()` while the list selection stayed intact. Restore nodes from the live
        // selection so all element options show again instead of only the create option.
        effect(() => {
            if (this.sidebarOpened() && !untracked(() => this.nodes())?.length) {
                const instance = this.nodeEntriesGlobalService.getPrimaryInstance();
                const selected = instance?.selection?.selected;
                if (selected?.length) {
                    this.nodes.set(selected);
                }
            }
        });
    }

    registerSidebar(editorialSidebar: EditorialSidebarComponent) {
        if (this._editorialSidebar && this._editorialSidebar !== editorialSidebar) {
            console.error(
                'Duplicate registration of editorial sidebar',
                this._editorialSidebar,
                editorialSidebar,
            );
            throw new Error('Duplicate registration of editorial sidebar');
        }
        this._editorialSidebar = editorialSidebar;
        this.sidebarAvailable.set(true);
    }

    unregisterSidebar(editorialSidebar: EditorialSidebarComponent) {
        if (this._editorialSidebar !== editorialSidebar) {
            throw new Error('This sidebar is not registered');
        }
        this._editorialSidebar = null;
        this.sidebarAvailable.set(false);
    }
    get editorialSidebar(): EditorialSidebarComponent {
        return this._editorialSidebar;
    }

    showOption(state: OptionState<OptionConfig>) {
        // only drop the fullscreen view when actually switching to a *different* option;
        // re-showing the same option (stepping the preview) keeps it.
        const optionChanged = this._editorialSidebar.enabledOption()?.option !== state.option;
        this._editorialSidebar.enabledOption.set(state);
        this.sidebarOpened.set(true);
        if (optionChanged) {
            this.showFullscreenToggle.set(false);
            this.fullscreenActive.set(false);
        }
    }

    patchOptionConfig(optionConfig: OptionConfig) {
        this.configChange$.emit(optionConfig);
        this._editorialSidebar.enabledOption.set({
            ...this._editorialSidebar.enabledOption(),
            optionConfig,
        });
    }

    close() {
        this._editorialSidebar?.enabledOption?.set(null);
        this.sidebarOpened.set(false);
        this.showFullscreenToggle.set(false);
        this.fullscreenActive.set(false);
        // The service is providedIn:'root' and shared across pages, so the selected nodes must be
        // reset too — otherwise the stale selection survives a scope change (e.g. search → collections).
        this.nodes.set(null);
    }

    /**
     * handle a select event from the node entries wrapper component and trigger the sidebar
     *
     * @param allowDeselect when false, clicking the already (solely) selected element keeps it
     * selected instead of toggling it off
     */
    handleSelect(
        nodeEntriesRef: NodeEntriesWrapperComponent<NodeEntriesDataType> | undefined,
        event: NodeClickEvent<NodeEntriesDataType>,
        scope: Scope,
        previewConfig?: PreviewConfig,
        allowDeselect = true,
    ) {
        this.scope.set(scope);
        const isSoleSelection =
            nodeEntriesRef?.getSelection()?.selected.length === 1 &&
            nodeEntriesRef?.getSelection()?.selected[0] === event.element;
        if (!isSoleSelection) {
            nodeEntriesRef?.getSelection()?.clear();
            this.sidebarOpened.set(false);
        }
        if (isSoleSelection && !allowDeselect) {
            nodeEntriesRef?.getSelection()?.select(event.element as Node);
        } else {
            nodeEntriesRef?.getSelection()?.toggle(event.element as Node);
        }
        if (nodeEntriesRef?.getSelection()?.selected.length === 0) {
            this.sidebarOpened.set(false);
        } else if (
            nodeEntriesRef?.getSelection()?.selected.length === 1 &&
            (event.element as Node).mediatype &&
            !['collection', 'folder'].includes((event.element as Node).mediatype)
        ) {
            this.nodes.set([event.element]);
            this.showOption({
                option: 'PREVIEW',
                trap: false,
                optionConfig: previewConfig,
            });
        }
    }

    /**
     * Programmatically select one or more nodes and surface their options in the sidebar.
     *
     * The nodes are added to the primary list as virtual nodes before selecting, so the list
     * itself reflects the selection. This matters for lists that do not otherwise contain the
     * nodes (e.g. the search results grid only holds nodes matching the current query, so a
     * freshly uploaded node would be selected in the model but invisible in the list)
     */
    selectNode(nodes: Node | Node[]) {
        const instance = this.nodeEntriesGlobalService.getPrimaryInstance();
        if (!instance) {
            return;
        }
        const nodeList = Array.isArray(nodes) ? nodes : [nodes];
        instance.list?.addVirtualNodes(nodeList, { select: true });
        this.nodes.set(nodeList);
        this._editorialSidebar?.enabledOption.set(null);
        this.sidebarOpened.set(true);
    }

    handleSelection(selection: SelectionChange<NodeEntriesDataType>) {
        this.nodes.set(selection.source.selected);
        const option = this._editorialSidebar.enabledOption()?.option;
        const selectionMode = option ? this.getOptionDescriptor(option).selectionMode : 'none';
        const count = selection.source.selected.length;
        // `any` outlives every selection change: such an option may have been opened by a host right
        // when nodes were added, and the list selects them immediately afterwards — closing on that
        // would drop the option in the same moment it appeared.
        if (selectionMode === 'any') {
            return;
        }
        if (count === 0) {
            this.close();
        } else if (selectionMode === 'none' || (selectionMode === 'single' && count > 1)) {
            // Only leave the current option when the new selection is no longer valid for it
            // (an option that ignores selection, or more than one node for a single-selection
            // option). Merely switching between valid selections keeps the option open — e.g.
            // stepping the preview to the previous/next node.
            this._editorialSidebar.enabledOption.set(null);
        }
    }

    /**
     * Resolve the index of the currently previewed node within the page's primary
     * node-entries list (the "selection" the preview was opened from).
     * Returns -1 when there is no primary list or the node is not part of it.
     */
    private getPreviewIndex(): { data: NodeEntriesDataType[]; index: number } {
        const data =
            this.nodeEntriesGlobalService.getPrimaryInstance()?.dataSource?.getData() ?? [];
        const current = this.nodes()?.[0] as Node;
        const index = current
            ? data.findIndex((n) => (n as Node)?.ref?.id === current.ref?.id)
            : -1;
        return { data, index };
    }

    /**
     * Whether a preview step by `offset` (e.g. -1 / +1) lands on an existing node
     * of the primary list. Used to enable/disable the preview navigation buttons.
     */
    canStepPreview(offset: number): boolean {
        const { data, index } = this.getPreviewIndex();
        return index >= 0 && !!data[index + offset];
    }

    /**
     * Move the preview to the previous/next node of the primary list and keep the
     * list selection in sync. `handleSelection` keeps the PREVIEW option open for a
     * single-node selection, so the enabled option is left untouched here — its
     * reference stays stable and the fullscreen view is preserved while stepping.
     */
    stepPreview(offset: number): void {
        const instance = this.nodeEntriesGlobalService.getPrimaryInstance();
        const { data, index } = this.getPreviewIndex();
        const target = index >= 0 ? data[index + offset] : undefined;
        if (!instance || !target) {
            return;
        }
        // Use setSelection (single change emission) rather than clear()+select(): clear() would
        // momentarily emit an empty selection, and `handleSelection` closes the sidebar on length 0.
        instance.selection.setSelection(target);
        this.nodes.set([target]);
    }
}
