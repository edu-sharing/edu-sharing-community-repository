import {
    Component,
    computed,
    effect,
    EventEmitter,
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
    OptionItem,
    OptionsHelperDataService,
    Target,
    UIAnimation,
    UIConstants,
    UIService,
} from 'ngx-edu-sharing-ui';
import { BehaviorSubject, Subject } from 'rxjs';
import { EditorialSidebarService } from './editorial-sidebar.service';
import { trigger } from '@angular/animations';
import { NodesSelectorConfig } from '../../pages/editorial-page/nodes-selector/nodes-selector.component';
import { CardDialogRef } from '../dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../dialogs/dialogs.service';
import {
    AssignmentConfig,
    SubmissionConfig,
} from '../../pages/editorial-page/submission-sidebar/submission-sidebar.component';

export type PrimaryMode = 'activity' | 'share' | 'assignment' | 'suggestions';
export type MainComponentType = 'manageAssignment' | 'assignmentSubmission' | 'submitAssignment';

export type SidebarContext = PrimaryMode | 'collections' | 'workspace' | 'search';
export type EditorialSidebarOption =
    | 'WORKSPACE_METADATA'
    | 'SHARE_QR'
    | 'PREVIEW'
    // sort into from right to left
    | 'SORT_INTO'
    // manage content is sort into from left to right
    | 'MANAGE_CONTENT'
    | 'MANAGE_SUBMISSION'
    | 'VIEW_ASSIGNMENT';

/**
 * list of options that support multi selection
 */
export const MULTISELECT_OPTIONS: EditorialSidebarOption[] = ['SORT_INTO'];

export type OptionConfig = NodesSelectorConfig | SubmissionConfig | AssignmentConfig;
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
    providers: [OptionsHelperDataService],
    animations: [trigger('overlay', UIAnimation.openOverlay())],
})
export class EditorialSidebarComponent implements OnInit, OnChanges, OnDestroy {
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
     * trigger to inform the editorial page to show a main component
     */
    @Output() showComponent = new EventEmitter<MainComponentType>();

    constructor(
        private dialogs: DialogsService,
        private uiService: UIService,
        private nodeHelperService: NodeHelperService,
        public editorialSidebarService: EditorialSidebarService,
        private optionsHelperDataService: OptionsHelperDataService,
    ) {
        this.editorialSidebarService.registerSidebar(this);
        effect(() => {
            this.editorialSidebarService.nodes();
            void this.initOptions();
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

        const workspaceMetadata = new OptionItem(
            'EDITORIAL.OPTIONS.WORKSPACE_METADATA',
            'info',
            () => this.enabledOption.set({ trap: false, option: 'WORKSPACE_METADATA' }),
        );
        workspaceMetadata.elementType = [ElementType.Node];
        workspaceMetadata.constrains = [Constrain.NoBulk, Constrain.HomeRepository];
        workspaceMetadata.scopes = ['workspace'];
        options.push(workspaceMetadata);

        const preview = new OptionItem('EDITORIAL.OPTIONS.PREVIEW', 'preview', () =>
            this.enabledOption.set({ trap: false, option: 'PREVIEW' }),
        );
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
        manageContent.scopes = ['workspace', 'collections', 'search'];
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
