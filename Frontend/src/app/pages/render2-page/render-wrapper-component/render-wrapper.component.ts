import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    signal,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import {
    ActionbarComponent,
    CombinedRenderData,
    EduSharingUiModule,
    OptionsHelperDataService,
    RenderHelperService,
    TranslationsService,
} from 'ngx-edu-sharing-ui';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { RenderComponent, RenderingServiceLibModule } from 'ngx-rendering-service-lib';
import { MdsModule } from '../../../features/mds/mds.module';
import { SharedModule } from '../../../shared/shared.module';
import { Node, NodeService, RestConstants } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'es-render-wrapper-component',
    templateUrl: 'render-wrapper.component.html',
    styleUrls: ['render-wrapper.component.scss'],
    imports: [
        CommonModule,
        EduSharingUiModule,
        MatButtonModule,
        RenderComponent,
        SharedModule,
        RenderingServiceLibModule,
        MdsModule,
    ],
    // required for optional mds module
    providers: [OptionsHelperDataService, RenderHelperService],
})
export class RenderWrapperComponent implements OnChanges {
    @ViewChild(ActionbarComponent) actionbar: ActionbarComponent;
    @Input() showTopbar = true;
    @Input() showMetadata = true;
    @Input() nodeId: string;
    @Input() repository: string;
    @Input() version: string;
    @Input() childId: string = null;
    @Output() childIdChange = new EventEmitter<string>();

    data = signal<CombinedRenderData>(null);
    loading = signal(false);
    children = signal<Node[]>(null);

    constructor(
        private renderHelperService: RenderHelperService,
        private nodeService: NodeService,
        private translations: TranslationsService,
        private optionsHelper: OptionsHelperDataService,
    ) {
        this.translations.waitForInit().subscribe(() => {});
        this.optionsHelper.registerGlobalKeyboardShortcuts();
        this.data.set(undefined);
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.nodeId) {
            this.children.set(
                (
                    await firstValueFrom(
                        this.nodeService.getChildren(changes.nodeId.currentValue, {
                            repository: this.repository,
                            filter: ['files'],
                            sortProperties: [RestConstants.CCM_PROP_CHILDOBJECT_ORDER],
                            sortAscending: [true],
                            assocName: RestConstants.CCM_ASSOC_CHILDIO,
                            maxItems: RestConstants.COUNT_UNLIMITED,
                        }),
                    )
                ).nodes,
            );
        }
        if (changes.nodeId || changes.childId) {
            await this.refresh();
        }
    }
    async refresh() {
        await this.setNodeById(this.childId || this.nodeId);
    }
    private async setNodeById(nodeId: string) {
        this.loading.set(true);
        delete this.data()?.request;
        this.data.set(this.data());
        const data = await this.renderHelperService.getRenderData(
            nodeId,
            this.version,
            this.repository,
        );
        setTimeout(async () => {
            await this.optionsHelper.initComponents(this.actionbar);
            await this.optionsHelper.refreshComponents();
        });
        this.loading.set(false);
        this.data.set(data);
    }

    setChild(child: Node = null) {
        this.childIdChange.emit(child?.ref?.id || null);
        // void this.setNodeById(child ? child.ref.id : this.nodeId);
    }
    goBack() {
        window.history.back();
    }
}
