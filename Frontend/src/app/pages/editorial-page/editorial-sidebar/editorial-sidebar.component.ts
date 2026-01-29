import {
    Component,
    computed,
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
import { Node, RestConstants, Submission } from 'ngx-edu-sharing-api';
import {
    Constrain,
    DefaultGroups,
    EduSharingUiCommonModule,
    ElementType,
    NodeEntriesDataType,
    NodeHelperService,
    OptionItem,
    OptionsHelperDataService,
    Target,
    UIConstants,
    UIService,
} from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { BehaviorSubject, Subject } from 'rxjs';
import { CardDialogRef } from '../../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { MainComponentType, PrimaryMode } from '../editorial-page.component';
import {
    NodesSelectorComponent,
    NodesSelectorConfig,
} from '../nodes-selector/nodes-selector.component';
import { MetadataSidebarComponent } from '../../workspace-page/metadata/metadata-sidebar.component';
import { PreviewSidebarModule } from '../../../features/preview-sidebar/preview-sidebar.module';
import { EditorialSidebarService } from './editorial-sidebar.service';
import { CdkMonitorFocus } from '@angular/cdk/a11y';
import {
    SubmissionConfig,
    SubmissionSidebarComponent,
} from '../submission-sidebar/submission-sidebar.component';
export type SidebarContext = PrimaryMode | 'collections' | 'workspace' | 'search';
export type EditorialSidebarOption =
    | 'WORKSPACE_METADATA'
    | 'SHARE_QR'
    | 'PREVIEW'
    | 'SORT_INTO'
    | 'MANAGE_SUBMISSION';
export type OptionState<T extends NodesSelectorConfig | SubmissionConfig> = {
    option: EditorialSidebarOption;
    /**
     * additional, optional state for the option
     * This might vary by the specific option
     */
    optionState?: any;
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
    imports: [
        EduSharingUiCommonModule,
        CdkMonitorFocus,
        CommonModule,
        MatButtonModule,
        TranslateModule,
        SubmissionSidebarComponent,
        NodesSelectorComponent,
        MetadataSidebarComponent,
        PreviewSidebarModule,
    ],
    providers: [OptionsHelperDataService],
})
export class EditorialSidebarComponent implements OnInit, OnChanges, OnDestroy {
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    parent = input<Node>();
    /**
     * current main component (only for editorial page context)
     */
    component = input<MainComponentType>();
    nodes = input<NodeEntriesDataType[]>();
    primaryMode = input.required<SidebarContext>();
    enabledOption = signal<OptionState<unknown>>(null);
    isModal = input<boolean>(false);

    @Output() closeTrigger = new EventEmitter<void>();
    @ViewChild('content', { static: true }) dialogContent: TemplateRef<unknown>;

    private readonly destroyed = new Subject<void>();
    readonly title = computed(() =>
        this.enabledOption()
            ? 'EDITORIAL.OPTIONS.' + this.enabledOption().option
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
        private editorialSidebarService: EditorialSidebarService,
        private optionsHelperDataService: OptionsHelperDataService,
    ) {
        this.editorialSidebarService.registerSidebar(this);
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
        workspaceMetadata.constrains = [Constrain.NoBulk];
        workspaceMetadata.scopes = ['workspace'];
        options.push(workspaceMetadata);

        const preview = new OptionItem('EDITORIAL.OPTIONS.PREVIEW', 'preview', () =>
            this.enabledOption.set({ trap: false, option: 'PREVIEW' }),
        );
        preview.elementType = [ElementType.Node];
        preview.constrains = [Constrain.NoBulk, Constrain.Files];
        // preview.scopes = ['workspace', 'collections'];
        options.push(preview);

        this.optionsHelperDataService.setData({
            scope: this.primaryMode(),
            activeObjects: this.nodes(),
            selectedObjects: this.nodes(),
            allObjects: this.nodes(),
            customOptions: {
                useDefaultOptions: false,
                addOptions: options,
            },
        });
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
        sortInto.customShowCallback = async () =>
            this.parent() && this.nodeHelperService.isNodeCollection(this.parent());
        sortInto.elementType = [ElementType.NoneOrUnknown];
        sortInto.scopes = ['collections'];
        options.push(sortInto);
        this.optionsHelperDataService.setData({
            scope: this.primaryMode(),
            activeObjects: this.nodes(),
            selectedObjects: this.nodes(),
            allObjects: this.nodes(),
            customOptions: {
                useDefaultOptions: false,
                addOptions: options,
            },
        });
        const options$ = new BehaviorSubject(
            await this.optionsHelperDataService.getAvailableOptions(Target.Actionbar),
        );
        void this.uiService.updateOptionEnabledState(options$);
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

    private close() {
        this.editorialSidebarService.sidebarOpened.set(false);
    }
}
