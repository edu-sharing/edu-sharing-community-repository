import {
    Component,
    computed,
    effect,
    EventEmitter,
    inject,
    input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    signal,
    SimpleChanges,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { Node, RestConstants, ROOT } from 'ngx-edu-sharing-api';
import {
    Constrain,
    DefaultGroups,
    ElementType,
    HideMode,
    NodeHelperService,
    NodesRightMode,
    OptionItem,
    OptionsHelperDataService,
    Target,
    UIAnimation,
    UIConstants,
    UIService,
} from 'ngx-edu-sharing-ui';
import { BehaviorSubject, Subject } from 'rxjs';
import { EditorialSidebarService } from './editorial-sidebar.service';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { provideReusableOptionsHelperData } from '../../services/options-helper-data.provider';
import { trigger } from '@angular/animations';
import { BreakpointObserver } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { NodesSelectorConfig } from '../../pages/editorial-page/nodes-selector/nodes-selector.component';
import { CardDialogRef } from '../dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../dialogs/dialogs.service';
import {
    AssignmentConfig,
    SubmissionConfig,
} from '../../pages/editorial-page/submission-sidebar/submission-sidebar.component';
import { EditorMode } from '../mds/types/types';

export type PrimaryMode = 'activity' | 'share' | 'assignment' | 'suggestions';
export type MainComponentType = 'manageAssignment' | 'assignmentSubmission' | 'submitAssignment';

export type SidebarContext = PrimaryMode | 'collections' | 'workspace' | 'search' | 'render';
export type SelectionMode = 'none' | 'single' | 'multi';

export type EditorialSidebarOptionDescriptor = {
    selectionMode: SelectionMode;
};

export const EDITORIAL_SIDEBAR_OPTIONS = {
    VERSION_MANAGEMENT: { selectionMode: 'single' },
    VIEWS_AND_USAGE: { selectionMode: 'multi' },
    SHARE_QR: { selectionMode: 'single' },
    PREVIEW: { selectionMode: 'single' },
    /** sort into from right to left (i.e. also create or upload) */
    SORT_INTO: { selectionMode: 'none' },
    /** manage content is sort into from left to right ("Einsortieren") */
    MANAGE_CONTENT: { selectionMode: 'multi' },
    MANAGE_SUBMISSION: { selectionMode: 'single' },
    VIEW_ASSIGNMENT: { selectionMode: 'single' },
} as const satisfies Record<string, EditorialSidebarOptionDescriptor>;

export type EditorialSidebarOption = keyof typeof EDITORIAL_SIDEBAR_OPTIONS;

export type PreviewConfig = {
    /** override the editorMode of the embedded mds-editor-wrapper. Default: 'viewer'. */
    editorMode?: EditorMode;
    /** override the groupId of the embedded mds-editor-wrapper. Default: 'preview_sidebar'. */
    groupId?: string;
};
export type OptionConfig =
    | NodesSelectorConfig
    | SubmissionConfig
    | AssignmentConfig
    | PreviewConfig;
export type OptionState<T extends OptionConfig> = {
    option: EditorialSidebarOption;
    /**
     * custom title to show in sidebar
     */
    title?: string;
    /**
     * any valid option config, varies for the selected option
     */
    optionConfig?: T;
    /**
     * when true, do not allow to navigate back to the overview of all actions
     */
    trap: boolean;
};
@Component({
    selector: 'es-editorial-sidebar',
    templateUrl: 'editorial-sidebar.component.html',
    styleUrls: ['editorial-sidebar.component.scss'],
    standalone: false,
    providers: [provideReusableOptionsHelperData()],
    animations: [trigger('overlay', UIAnimation.openOverlay())],
    host: {
        '[class.fullscreen]': 'editorialSidebarService.fullscreenActive()',
    },
})
export class EditorialSidebarComponent implements OnInit, OnChanges, OnDestroy {
    private dialogs = inject(DialogsService);
    private uiService = inject(UIService);
    private nodeHelperService = inject(NodeHelperService);
    editorialSidebarService = inject(EditorialSidebarService);
    private optionsHelperDataService = inject(OptionsHelperDataService);
    private breakpointObserver = inject(BreakpointObserver);
    private loadingScreenService = inject(LoadingScreenService);
    /** hide the toggle tab while the global loading screen covers the app */
    readonly isLoading = toSignal(this.loadingScreenService.observeIsLoading(), {
        initialValue: true,
    });

    /** true on desktop; below 900px ($mobileSidebarModal) the sidebar is a full-screen overlay */
    readonly isDesktop = toSignal(
        this.breakpointObserver.observe(['(max-width: 900px)']).pipe(map((r) => !r.matches)),
        { initialValue: true },
    );

    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    parent = input<Node>();
    /**
     * current main component (only for editorial page context)
     */
    component = input<MainComponentType>();
    primaryMode = input.required<SidebarContext>();
    enabledOption = signal<OptionState<unknown>>(null);
    isModal = input<boolean>(false);

    //@Output() closeTrigger = new EventEmitter<void>();
    @ViewChild('content', { static: true }) dialogContent: TemplateRef<unknown>;

    private readonly destroyed = new Subject<void>();
    readonly title = computed(() =>
        this.enabledOption()
            ? this.enabledOption().title || 'EDITORIAL.OPTIONS.' + this.enabledOption().option
            : 'EDITORIAL.SIDEBAR.TITLE_' + this.primaryMode()?.toUpperCase(),
    );
    options = signal<OptionItem[]>(null);
    /**
     * Whether the sidebar has anything to show: either a specific option is open, or the option
     * list is non-empty. `options() === null` means "not computed yet" (loading) and is treated as
     * no-content so the tab doesn't flash. Used to hide the open/close tab and to avoid opening the
     * panel to just the "no options" message.
     */
    readonly hasContent = computed(
        () => !!this.enabledOption() || (this.options()?.length ?? 0) > 0,
    );
    /**
     * trigger to inform the editorial page to show a main component
     */
    @Output() showComponent = new EventEmitter<MainComponentType>();

    constructor() {
        this.editorialSidebarService.registerSidebar(this);
        effect(() => {
            this.editorialSidebarService.nodes();
            void this.initOptions();
        });
        // reset fullscreen whenever an option is newly opened / closed / switched
        effect(() => {
            this.enabledOption();
            this.editorialSidebarService.fullscreenActive.set(false);
        });
        // closing the sidebar (via the tab or any other path) resets the opened option, so a later
        // open starts at the option overview instead of the previously shown option
        effect(() => {
            if (!this.editorialSidebarService.sidebarOpened()) {
                this.enabledOption.set(null);
            }
        });
        // never leave the panel open on an empty "no options" state: once the options have been
        // computed (options() !== null) and there is nothing to show, close it. Guarded on the
        // resolved (non-null) options so a recompute doesn't momentarily close a valid sidebar.
        effect(() => {
            if (
                this.editorialSidebarService.sidebarOpened() &&
                this.options() !== null &&
                !this.hasContent()
            ) {
                this.editorialSidebarService.sidebarOpened.set(false);
            }
        });
    }

    async ngOnChanges(changes: SimpleChanges) {
        const options = await this.initOptions();
        this.enableDefaultOption(changes, options);
    }

    ngOnInit(): void {
        if (this.isModal()) {
            void this.openDialog();
        }
        void this.initOptions();
    }

    private async initOptions() {
        // mark as "loading" so hasContent()/the auto-close effect don't act on a stale list while
        // the new options are (asynchronously) computed
        this.options.set(null);
        const options = [];
        const shareElement = new OptionItem('EDITORIAL.OPTIONS.SHARE_QR', 'share', (nodes) =>
            this.dialogs.openQrDialog({
                node: nodes[0],
            }),
        );
        shareElement.elementType = [ElementType.Node];
        shareElement.constrains = [Constrain.NoBulk];
        shareElement.scopes = ['activity'];
        options.push(shareElement);

        const versionManagement = new OptionItem(
            'EDITORIAL.OPTIONS.VERSION_MANAGEMENT',
            'info',
            () => this.enabledOption.set({ trap: false, option: 'VERSION_MANAGEMENT' }),
        );
        versionManagement.elementType = [ElementType.Node];
        versionManagement.constrains = [Constrain.NoBulk, Constrain.HomeRepository];
        versionManagement.scopes = ['workspace', 'search', 'suggestions'];
        versionManagement.permissions = [RestConstants.PERMISSION_WRITE];
        versionManagement.permissionsRightMode = NodesRightMode.Effective;
        versionManagement.permissionsMode = HideMode.Hide;
        options.push(versionManagement);

        const showStatistics = new OptionItem(
            'EDITORIAL.OPTIONS.VIEWS_AND_USAGE',
            'bar_chart',
            () => this.enabledOption.set({ trap: false, option: 'VIEWS_AND_USAGE' }),
        );
        showStatistics.constrains = [Constrain.HomeRepository, Constrain.User];
        showStatistics.scopes = ['workspace', 'search', 'collections', 'suggestions', 'render'];
        showStatistics.toolpermissions = [RestConstants.TOOLPERMISSION_SELECTIVE_STATISTICS_NODES];
        showStatistics.toolpermissionsMode = HideMode.Hide;
        showStatistics.group = DefaultGroups.View;
        options.push(showStatistics);
        const preview = new OptionItem('EDITORIAL.OPTIONS.PREVIEW', 'preview', () =>
            this.enabledOption.set({
                trap: false,
                option: 'PREVIEW',
                optionConfig: {
                    editorMode:
                        this.optionsHelperDataService.getData().scope === 'suggestions'
                            ? 'nodes'
                            : 'viewer',
                } as PreviewConfig,
            }),
        );
        preview.customShowCallback = async () => {
            return this.optionsHelperDataService?.getData()?.scope !== 'render';
        };

        preview.group = DefaultGroups.View;
        preview.elementType = [ElementType.Node];
        preview.constrains = [Constrain.NoBulk, Constrain.Files];
        // preview.scopes = ['workspace', 'collections'];
        options.push(preview);

        const createAssignment = new OptionItem(
            'EDITORIAL.OPTIONS.CREATE_ASSIGNMENT',
            'task',
            () => {
                this.showComponent.emit('manageAssignment');
                this.close();
            },
        );
        createAssignment.group = DefaultGroups.Create;
        createAssignment.toolpermissions = [
            RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_ASSIGNMENTS,
        ];
        createAssignment.elementType = [ElementType.NoneOrUnknown];
        createAssignment.scopes = ['assignment'];
        // only show when no main component is active
        createAssignment.customShowCallback = async () => !this.component();
        options.push(createAssignment);

        const sortInto = new OptionItem(
            'EDITORIAL.OPTIONS.SORT_INTO',
            'splitscreen_vertical_add',
            () => this.enabledOption.set({ trap: false, option: 'SORT_INTO' }),
        );
        createAssignment.group = DefaultGroups.Primary;
        sortInto.customShowCallback = async () => {
            const validParent = this.parent() && ![ROOT].includes(this.parent().ref.id);
            const isMapOrFolder =
                this.parent()?.type &&
                [RestConstants.CM_TYPE_FOLDER, RestConstants.CCM_TYPE_MAP].includes(
                    this.parent().type,
                );
            return validParent && isMapOrFolder;
        };
        sortInto.permissions = [RestConstants.ACCESS_ADD_CHILDREN];
        sortInto.permissionsMode = HideMode.Disable;
        sortInto.elementType = [ElementType.NoneOrUnknown];
        sortInto.scopes = ['collections', 'workspace'];
        options.push(sortInto);

        const manageContent = new OptionItem(
            'EDITORIAL.OPTIONS.MANAGE_CONTENT',
            'tab_new_right',
            () =>
                this.enabledOption.set({
                    trap: false,
                    option: 'MANAGE_CONTENT',
                    optionConfig: { nodes: this.editorialSidebarService.nodes() },
                }),
        );
        manageContent.group = DefaultGroups.Edit;
        manageContent.elementType = [ElementType.Node];
        manageContent.constrains = [Constrain.Files];
        manageContent.scopes = ['workspace', 'collections', 'search', 'render'];
        options.push(manageContent);
        this.optionsHelperDataService.setData({
            scope: this.primaryMode(),
            activeObjects: this.editorialSidebarService.nodes(),
            selectedObjects: this.editorialSidebarService.nodes(),
            allObjects: this.editorialSidebarService.nodes(),
            customOptions: {
                useDefaultOptions: false,
                addOptions: options,
            },
        });
        const options$ = new BehaviorSubject(
            await this.optionsHelperDataService.getAvailableOptions(Target.Actionbar),
        );
        void this.uiService.updateOptionEnabledState(
            options$,
            this.parent() ? [this.parent()] : null,
        );
        this.options.set(options$.value);
        return options;
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
        this.editorialSidebarService.unregisterSidebar(this);
    }

    private async openDialog(): Promise<CardDialogRef<unknown>> {
        return await this.dialogs.openGenericDialog({
            title: this.title(),
            contentTemplate: this.dialogContent,
            contentPadding: 0,
            minWidth: 350,
        });
    }

    /**
     * checks if a default option should be triggered for the current state, and if so, does it
     */
    private enableDefaultOption(changes: SimpleChanges, options: OptionItem[]) {
        let optionId = null;
        let trap = false;
    }

    close() {
        this.editorialSidebarService.close();
    }
}
