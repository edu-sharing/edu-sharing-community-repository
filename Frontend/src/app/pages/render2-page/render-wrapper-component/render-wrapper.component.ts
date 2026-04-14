import {
    Component,
    ElementRef,
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
    /**
     * shall childobjects be shown (as a list at the top)
     */
    @Input() showChildobjects = false;
    @Input() nodeId: string;
    @Input() repository: string;
    @Input() version: string;
    @Input() childId: string = null;
    @Output() childIdChange = new EventEmitter<string>();

    @ViewChild('childobjects') childobjects: ElementRef;
    /**
     * updates via boxObserver
     * and holds the information if scrolling in the direction is currently feasible
     */
    scroll = {
        left: false,
        right: false,
    };
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
            if (this.showChildobjects) {
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
            } else {
                this.children.set(null);
            }
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

    private canScroll(direction: 'left' | 'right') {
        const element = this.childobjects?.nativeElement;
        if (element) {
            if (direction === 'left') {
                return element.scrollLeft > 0;
            } else if (direction === 'right') {
                /*
                 use a small pixel buffer (10px) because scrolling aligns with the start of each card and
                 it can cause slight alignment issues on the end of the container
                 */
                return element.scrollLeft < element.scrollWidth - element.clientWidth - 10;
            }
        }
        return false;
    }

    updateScrollState() {
        this.scroll.left = this.canScroll('left');
        this.scroll.right = this.canScroll('right');
    }
    doScroll(direction: 'left' | 'right') {
        // 1 is enough because the browser will handle it via css snapping
        const leftScroll = this.childobjects?.nativeElement.scrollLeft;
        const rect = this.childobjects?.nativeElement.getBoundingClientRect();
        // using scroll because it works more reliable than scrollBy
        this.childobjects?.nativeElement.scroll({
            left: leftScroll + Math.max(250, rect.width * 0.4) * (direction === 'right' ? 1 : -1),
            behavior: 'smooth',
        });
    }
}
