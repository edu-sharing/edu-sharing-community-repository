import {
    AfterViewInit,
    Component,
    Input,
    OnChanges,
    OnDestroy,
    signal,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { AboutService, Node } from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    CustomOptions,
    NodeHelperService,
    OptionsHelperDataService,
    RenderHelperService,
    Scope,
} from 'ngx-edu-sharing-ui';
import { CardDialogRef } from '../dialogs/card-dialog/card-dialog-ref';
import { Subject } from 'rxjs';
import { DialogsService } from '../dialogs/dialogs.service';
import { Router } from '@angular/router';
import { MdsEditorWrapperComponent } from '../mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { ModuleInfoService } from 'ngx-rendering-service-lib';
import { PreviewSidebarTemplateService } from './preview-sidebar-template.service';

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
    /**
     all modules in this list will be automatically rendered without confirmation
     */
    readonly AutoRenderModules = ['image', 'video', 'audio', 'document', 'pdf', 'url'];

    /**
     * always render the node, do not wait for click
     */
    @Input() autoRender = false;

    @Input() customOptions: CustomOptions;
    @ViewChild(ActionbarComponent) actionbar: ActionbarComponent;
    @ViewChild(MdsEditorWrapperComponent) mdsRef: MdsEditorWrapperComponent;

    private readonly destroyed = new Subject<void>();
    private _node: Node;
    renderNode = signal<Node>(null);

    /** The node to preview. */
    @Input()
    get node(): Node {
        return this._node;
    }

    set node(node: Node) {
        this._node = node;
        this.renderNode.set(null);
        this.allDetailsLink =
            (this.nodeHelper.getNodeLink('routerLink', node) as string) +
            '?' +
            Object.entries(this.nodeHelper.getNodeLink('queryParams', node))
                .filter((k) => !!k[1])
                .map((k) => k[0] + '=' + encodeURIComponent(k[1]))
                .join('&');
        void this.mdsRef?.reInit();
        if (this.actionbar) {
            void this.updateOptions();
        }
        this.about.hasPlugin('rendering-service-2').then(async (has) => {
            if (has) {
                const module = await this.moduleInfoService.getModuleInfo(node);
                console.info('rs module', module);
                if (this.autoRender || this.AutoRenderModules.includes(module.module)) {
                    void this.onShowContentClick();
                }
            } else {
                console.info('rs2 not present');
            }
        });
    }

    allDetailsLink: string;

    constructor(
        private nodeHelper: NodeHelperService,
        private dialogs: DialogsService,
        public optionsHelper: OptionsHelperDataService,
        public moduleInfoService: ModuleInfoService,
        public previewSidebarTemplateService: PreviewSidebarTemplateService,
        private renderHelperService: RenderHelperService,
        public router: Router,
        public about: AboutService,
    ) {
        void this.renderHelperService.prepareRootUrl();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.customOptions) {
            void this.updateOptions();
        }
    }

    ngAfterViewInit(): void {
        if (this.node) {
            void this.updateOptions();
        }
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    async onShowContentClick(): Promise<void> {
        if (await this.about.hasPlugin('rendering-service-2')) {
            this.renderNode.set(this.node);
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
            await this.router.navigateByUrl(this.allDetailsLink);
        }
    }

    private async openMediaDialog(): Promise<CardDialogRef> {
        return await this.dialogs.openPreviewMediaDialog({ node: this._node });
    }

    /**
     * Updates the actions bar's options to match the current node.
     *
     * Should be called when `this.node` changes but not until `actionbar` is available.
     */
    private async updateOptions() {
        await this.optionsHelper.initComponents(this.actionbar);
        this.optionsHelper.setData({
            scope: Scope.Search,
            activeObjects: [this.node],
            customOptions: this.customOptions,
        });
        void this.optionsHelper.refreshComponents();
    }
}
