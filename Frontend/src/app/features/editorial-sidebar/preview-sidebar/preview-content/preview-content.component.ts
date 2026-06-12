import {
    AfterViewInit,
    Component,
    effect,
    Input,
    input,
    OnChanges,
    OnDestroy,
    signal,
    SimpleChanges,
    ViewChild,
    inject,
    effect,
    input,
} from '@angular/core';
import { AboutService, NetworkService, Node } from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    CustomOptions,
    NodeHelperService,
    OptionItemToggle,
    OptionsHelperDataService,
    RenderHelperService,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom, Subject } from 'rxjs';
import { Params, Router } from '@angular/router';
import { ModuleInfoService } from 'ngx-rendering-service-lib';
import { PreviewSidebarTemplateService } from '../preview-sidebar-template.service';
import { MdsEditorWrapperComponent } from '../../../mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { EditorMode } from '../../../mds/types/types';
import { DialogsService } from '../../../dialogs/dialogs.service';
import { EditorialSidebarService } from '../../editorial-sidebar.service';
import { CardDialogRef } from '../../../dialogs/card-dialog/card-dialog-ref';

/**
 * The inner part of the preview sidebar.
 *
 * Will be used as dialog body on small screens by `PreviewSidebarComponent`.
 */
@Component({
    selector: 'es-preview-content',
    templateUrl: './preview-content.component.html',
    styleUrls: ['./preview-content.component.scss'],
    providers: [OptionsHelperDataService],
    standalone: false,
})
export class PreviewContentComponent implements AfterViewInit, OnDestroy, OnChanges {
    private nodeHelper = inject(NodeHelperService);
    private dialogs = inject(DialogsService);
    optionsHelper = inject(OptionsHelperDataService);
    moduleInfoService = inject(ModuleInfoService);
    previewSidebarTemplateService = inject(PreviewSidebarTemplateService);
    editorialSidebarService = inject(EditorialSidebarService);
    networkService = inject(NetworkService);
    private renderHelperService = inject(RenderHelperService);
    router = inject(Router);
    about = inject(AboutService);

    /**
     all modules in this list will be automatically rendered without confirmation
     */
    readonly AutoRenderModules = ['default', 'image', 'video', 'audio', 'document', 'pdf', 'url'];

    /**
     * always render the node, do not wait for click
     */
    @Input() autoRender = false;

    /**
     * whether this content is displayed inside the fullscreen modal rather than the sidebar
     */
    @Input() modal = false;

    @Input() customOptions: CustomOptions;
    /** Editor mode for the embedded mds-editor-wrapper. */
    @Input() editorMode: EditorMode = 'viewer';
    /** Group id for the embedded mds-editor-wrapper. */
    @Input() groupId: string = 'preview_sidebar';
    @ViewChild(ActionbarComponent) actionbar: ActionbarComponent;
    @ViewChild(MdsEditorWrapperComponent) mdsRef: MdsEditorWrapperComponent;

    private readonly destroyed = new Subject<void>();
    readonly node = input<Node>();
    renderNode = signal<Node>(null);
    mdsVisible = signal<boolean>(true);

    allDetailsLink: string;
    allDetailsParams: Params;

    constructor() {
        void this.renderHelperService.prepareRootUrl();
        effect(() => {
            const node = this.node();
            this.renderNode.set(null);
            this.mdsVisible.set(false);
            setTimeout(() => this.mdsVisible.set(true));
            this.allDetailsParams = this.nodeHelper.getNodeLink('queryParams', node) as Params;
            this.allDetailsLink = this.nodeHelper.getNodeLink('routerLink', node) as string;
            queueMicrotask(() => void this.mdsRef?.reInit());
            if (this.actionbar) {
                void this.updateOptions();
            }
            if (node) {
                void this.about.hasPlugin('rendering-service-2').then(async (has) => {
                    if (has) {
                        let module = 'default';
                        if (await firstValueFrom(this.networkService.isFromHomeRepository(node))) {
                            // in this stage, we don't know external rs2 url so we can only resolve it for the local rs2
                            module = (await this.moduleInfoService.getModuleInfo(node)).module;
                        }
                        console.info('rs module', module);
                        if (this.autoRender || this.AutoRenderModules.includes(module)) {
                            void this.onShowContentClick();
                        }
                    } else {
                        console.info('rs2 not present');
                    }
                });
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.customOptions) {
            void this.updateOptions();
        }
    }

    ngAfterViewInit(): void {
        if (this.node()) {
            void this.updateOptions();
        }
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    async onShowContentClick(): Promise<void> {
        if (await this.about.hasPlugin('rendering-service-2')) {
            this.renderNode.set(this.node());
            /*let dialogRefPromise: Promise<CardDialogRef>;

            dialogRefPromise = this.openMediaDialog();
            const dialogRef = await dialogRefPromise;
            dialogRef.afterClosed().subscribe(() => {
                dialogRefPromise = null;
                if (!this.destroyed.isStopped) {
                    //this.closed.emit();
                }
            });
            this.destroyed.subscribe(() => {
                void dialogRefPromise?.then((dialogRef) => dialogRef.close());
            });*/
        } else {
            await this.router.navigate([this.allDetailsLink], {
                queryParams: this.allDetailsParams,
            });
        }
    }

    private async openMediaDialog(): Promise<CardDialogRef> {
        return await this.dialogs.openPreviewMediaDialog({ node: this.node() });
    }

    async onSaveMds(): Promise<void> {
        await this.mdsRef?.onSave();
    }

    /**
     * Updates the actions bar's options to match the current node.
     *
     * Should be called when `this.node` changes but not until `actionbar` is available.
     */
    private async updateOptions() {
        await this.optionsHelper.initComponents(this.actionbar);
        this.optionsHelper.setData({
            scope: this.editorialSidebarService.scope(),
            activeObjects: [this.node],
            customOptions: this.customOptions,
            postPrepareOptions: (options) => {
                // no toggles in sidebar
                options.splice(
                    0,
                    options.length,
                    ...options.filter((o) => !(o as OptionItemToggle).isToggle),
                );
            },
        });
        void this.optionsHelper.refreshComponents();
    }
}
