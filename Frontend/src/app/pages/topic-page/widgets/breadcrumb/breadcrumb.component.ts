import { CommonModule } from '@angular/common';
import {
    Component,
    ComponentRef,
    computed,
    effect,
    ElementRef,
    EventEmitter,
    input,
    Input,
    InputSignal,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    signal,
    Signal,
    SimpleChanges,
    ViewChild,
    ViewContainerRef,
    WritableSignal,
    inject,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';
import { Node, ParentEntries } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, UIService } from 'ngx-edu-sharing-ui';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RestConnectorService } from '../../../../core-module/rest/services/rest-connector.service';
import { TopicPageGlobalService } from '../../shared/services/topic-page-global.service';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { DEFAULT_PAGE_VARIANT_NAME_PREFIX } from '../../shared/types/custom-definitions';
import { BreadcrumbConfig } from '../../shared/types/widget-config/breadcrumb-config';
import { retrieveWidgetConfigFromNode } from '../../shared/utils/template-util';

export interface BreadcrumbExtensionInterface {
    // inputs
    editorialMemberNodeIds: string[];
    editMode: InputSignal<boolean>;

    // outputs
    editorialMemberNodeIdsChanged: EventEmitter<string[]>;
}

@Component({
    selector: 'es-topic-page-breadcrumb',
    imports: [
        CommonModule,
        EduSharingUiCommonModule,
        MatButtonModule,
        MatFormFieldModule,
        MatMenuModule,
        MatSelectModule,
        TranslateModule,
    ],
    templateUrl: './breadcrumb.component.html',
    styleUrls: ['./breadcrumb.component.scss'],
})
export class BreadcrumbComponent implements OnChanges, OnInit, OnDestroy {
    private connector = inject(RestConnectorService);
    private topicPageGlobalService = inject(TopicPageGlobalService);
    private topicPageHelperService = inject(TopicPageHelperService);
    private uiService = inject(UIService);

    // CONSTANTS
    readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.BREADCRUMB.';
    readonly navigateToTemplateValue: string = 'NAVIGATE_TO_TEMPLATE';

    // INPUTS
    @Input() contextNodeId: string;
    editMode: InputSignal<boolean> = input<boolean>(false);
    isProcessing: InputSignal<boolean> = input<boolean>(false);
    @Input() nodeId?: string;
    // propagated (inherited) breadcrumb node — used to render the editorial members as selected
    // while still letting an edit create a fresh copy (persistConfig keys off the empty nodeId)
    @Input() propagatedNodeId?: string;
    @Input() pageVariantNode?: Node;
    pageTemplateExists: InputSignal<boolean> = input<boolean>(false);
    pageVariantConfigNodes: InputSignal<Node[]> = input<Node[]>([]);
    parentEntries: InputSignal<ParentEntries> = input<ParentEntries>(null);
    @Input() rootName?: string;
    templateMode: InputSignal<boolean> = input<boolean>(false);
    @Output() navigateToTemplate: EventEmitter<void> = new EventEmitter<void>();
    @Output() variantSelected: EventEmitter<string> = new EventEmitter<string>();

    // VARIABLES
    customUrl: (node: Node) => string;
    customUrlTarget: '_self' | '_blank' = '_self';
    private destroy$ = new Subject<void>();
    editorialMemberNodeIds: string[] = [];
    hasBreadcrumbExtension: WritableSignal<boolean> = signal(false);
    initialized: WritableSignal<boolean> = signal(false);
    isAdmin: WritableSignal<boolean> = signal(false);
    // reverse Array to get the correct order for breadcrumb
    processedNodes: Signal<Node[]> = computed((): Node[] => {
        return this.parentEntries()?.nodes?.slice()?.reverse() || [];
    });
    rootLink: WritableSignal<string> = signal('');
    widgetConfig: BreadcrumbConfig;

    // CUSTOM EXTENSION RELATED VARIABLES
    @ViewChild('customExtension', { read: ViewContainerRef, static: false })
    customExtension!: ViewContainerRef;
    @ViewChild('customExtension') customExtensionElement!: ElementRef<HTMLElement>;
    private customExtensionComponentRef: ComponentRef<any> | null = null;
    customExtensionInstance: BreadcrumbExtensionInterface;

    constructor() {
        effect((): void => {
            // update editMode of custom extension
            const currentEditMode: boolean = this.editMode();
            if (this.customExtensionComponentRef) {
                this.customExtensionComponentRef.setInput('editMode', currentEditMode);
            }
        });
        // wait for the login to be ready
        this.connector.isLoggedIn().subscribe((): void => {
            // check for administration privileges
            this.isAdmin.set(this.connector.getCurrentLogin()?.isAdmin ?? false);
        });
        this.hasBreadcrumbExtension.set(this.topicPageGlobalService.hasCustomBreadcrumbExtension());
        if (this.topicPageGlobalService.getCustomBreadcrumbRootLink()) {
            this.rootLink.set(this.topicPageGlobalService.getCustomBreadcrumbRootLink());
        }
        if (this.topicPageGlobalService.getCustomUrlFunction()) {
            this.customUrl = this.topicPageGlobalService.getCustomUrlFunction();
        }
        this.hasBreadcrumbExtension.set(this.topicPageGlobalService.hasCustomBreadcrumbExtension());
        this.customUrlTarget = this.topicPageGlobalService.getCustomUrlTarget();
    }

    /**
     * Initializes the breadcrumb component.
     */
    ngOnInit(): void {
        void this.initializeComponent();
    }

    /**
     * Reloads the widget config when the (propagated) node ID changes — e.g. when switching into
     * template mode or to another page variant — so a stale config from the previously loaded node
     * is not kept.
     *
     * @param changes
     */
    async ngOnChanges(changes: SimpleChanges): Promise<void> {
        const nodeIdChanged: boolean =
            !!changes.nodeId &&
            !changes.nodeId.firstChange &&
            changes.nodeId.currentValue !== changes.nodeId.previousValue;
        const propagatedNodeIdChanged: boolean =
            !!changes.propagatedNodeId &&
            !changes.propagatedNodeId.firstChange &&
            changes.propagatedNodeId.currentValue !== changes.propagatedNodeId.previousValue;
        if (nodeIdChanged || propagatedNodeIdChanged) {
            await this.loadWidgetConfig();
        }
    }

    /**
     * On destruction, destroy custom extension component if it exists and complete destroy subject.
     */
    ngOnDestroy(): void {
        if (this.customExtensionComponentRef) {
            this.customExtensionComponentRef.destroy();
        }
        this.destroy$.next();
        this.destroy$.complete();
    }

    /**
     * Initializes the breadcrumb component.
     */
    async initializeComponent(): Promise<void> {
        await this.loadWidgetConfig();
        if (this.topicPageGlobalService.hasCustomBreadcrumbExtension()) {
            this.initializeCustomExtension();
        }
        // set component to be initialized
        this.initialized.set(true);
    }

    /**
     * Loads the breadcrumb widget config from the current (or propagated) node and pushes the
     * resulting editorial members into the custom extension when it is already mounted. Falling
     * back to the propagated (inherited) node shows the inherited editorial members as selected
     * until the page is adjusted.
     */
    private async loadWidgetConfig(): Promise<void> {
        const configNodeId: string = this.nodeId || this.propagatedNodeId;
        this.widgetConfig = configNodeId
            ? retrieveWidgetConfigFromNode(await this.topicPageHelperService.getNode(configNodeId))
            : undefined;
        this.editorialMemberNodeIds = this.widgetConfig?.editorialMemberNodeIds || [];
        // refresh the selected editorial members in the custom extension if it is already mounted
        if (this.customExtensionComponentRef) {
            this.customExtensionComponentRef.setInput(
                'editorialMemberNodeIds',
                this.editorialMemberNodeIds,
            );
        }
    }

    /**
     * Emits either the selected variantId or the navigate to template event to the parent component.
     *
     * @param variantId
     */
    selectVariant(variantId: string): void {
        if (variantId === this.navigateToTemplateValue) {
            this.navigateToTemplate.emit();
            return;
        }
        this.variantSelected.emit(variantId);
    }

    /**
     * Persists the currently defined config.
     */
    async persistConfig(): Promise<void> {
        // retrieve a widget config from the currently set variables
        this.widgetConfig = this.retrieveWidgetConfig();
        // persist config by creating a new or updating an existing node
        await this.topicPageHelperService.persistConfig(
            this.nodeId,
            -1,
            -1,
            this.pageVariantNode,
            this.widgetConfig,
            null,
            this.contextNodeId,
            true,
        );
    }

    // HELPERS
    /**
     * Helper function to retrieve a widget config from the currently set variables in the component.
     */
    private retrieveWidgetConfig(): BreadcrumbConfig {
        return {
            editorialMemberNodeIds: this.editorialMemberNodeIds,
        };
    }

    /**
     * Initializes the custom breadcrumb extension.
     */
    private initializeCustomExtension(): void {
        void this.topicPageGlobalService.getCustomBreadcrumbExtension().then((componentClass) => {
            // inject the component into the widget container
            this.customExtensionComponentRef = this.uiService.injectAngularComponent(
                this.customExtension,
                componentClass,
                this.customExtensionElement.nativeElement,
                {
                    editorialMemberNodeIds: this.widgetConfig?.editorialMemberNodeIds || [],
                } as unknown as Partial<BreadcrumbExtensionInterface>,
                { replace: false },
            );
            // separate set input of editMode as this is an InputSignal
            this.customExtensionComponentRef.setInput('editMode', this.editMode());
            this.customExtensionInstance = this.customExtensionComponentRef.instance;
            // listen to outputs of the custom extension instance
            this.setupCustomExtensionInstanceOutputs();
        });
    }

    /**
     * Registers actions that should be executed if a specific output is called.
     */
    private setupCustomExtensionInstanceOutputs(): void {
        if (!this.customExtensionInstance) {
            return;
        }
        this.customExtensionInstance.editorialMemberNodeIdsChanged
            ?.pipe(takeUntil(this.destroy$))
            .subscribe((editorialMembers: string[]) => {
                this.editorialMemberNodeIds = editorialMembers;
                void this.persistConfig();
            });
    }

    protected readonly pageVariantConfigPrefix = DEFAULT_PAGE_VARIANT_NAME_PREFIX;
}
