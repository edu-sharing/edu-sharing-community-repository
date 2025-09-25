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
import { Node } from 'ngx-edu-sharing-api';
import {
    Constrain,
    EduSharingUiCommonModule,
    ElementType,
    NodeHelperService,
    OptionItem,
    OptionsHelperDataService,
    Target,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { Subject } from 'rxjs';
import { CardDialogRef } from '../../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { PrimaryMode } from '../editorial-page.component';
import { NodesSelectorComponent } from '../nodes-selector/nodes-selector.component';
import { MetadataSidebarComponent } from '../../workspace-page/metadata/metadata-sidebar.component';
import { PreviewSidebarModule } from '../../../features/preview-sidebar/preview-sidebar.module';
export type SidebarContext = PrimaryMode | 'collections' | 'workspace' | 'search';
@Component({
    selector: 'es-editorial-sidebar',
    templateUrl: 'editorial-sidebar.component.html',
    styleUrls: ['editorial-sidebar.component.scss'],
    imports: [
        EduSharingUiCommonModule,
        CommonModule,
        MatButtonModule,
        TranslateModule,
        NodesSelectorComponent,
        MetadataSidebarComponent,
        PreviewSidebarModule,
    ],
    providers: [OptionsHelperDataService],
})
export class EditorialSidebarComponent implements OnInit, OnChanges, OnDestroy {
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    parent = input<Node>();
    nodes = input<Node[]>();
    primaryMode = input.required<SidebarContext>();
    enabledOption = signal<OptionItem>(null);
    isModal = input<boolean>(false);

    @Output() closeTrigger = new EventEmitter<void>();
    @ViewChild('content', { static: true }) dialogContent: TemplateRef<unknown>;

    private readonly destroyed = new Subject<void>();
    readonly title = computed(() =>
        this.enabledOption()
            ? this.enabledOption().name
            : 'EDITORIAL.SIDEBAR.TITLE_' + this.primaryMode()?.toUpperCase(),
    );
    options = signal<OptionItem[]>(null);

    constructor(
        private dialogs: DialogsService,
        private nodeHelperService: NodeHelperService,
        private optionsHelperDataService: OptionsHelperDataService,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        void this.initOptions();
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
            () => this.enabledOption.set(workspaceMetadata),
        );
        workspaceMetadata.elementType = [ElementType.Node];
        workspaceMetadata.constrains = [Constrain.NoBulk];
        workspaceMetadata.scopes = ['workspace'];
        options.push(workspaceMetadata);

        const preview = new OptionItem('EDITORIAL.OPTIONS.PREVIEW', 'preview', () =>
            this.enabledOption.set(preview),
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
        const sortInto = new OptionItem(
            'EDITORIAL.OPTIONS.SORT_INTO',
            'splitscreen_vertical_add',
            () => this.enabledOption.set(sortInto),
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
        this.options.set(await this.optionsHelperDataService.getAvailableOptions(Target.Actionbar));
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private async openDialog(): Promise<CardDialogRef<unknown>> {
        return await this.dialogs.openGenericDialog({
            title: this.title(),
            contentTemplate: this.dialogContent,
            contentPadding: 0,
            minWidth: 350,
        });
    }
}
