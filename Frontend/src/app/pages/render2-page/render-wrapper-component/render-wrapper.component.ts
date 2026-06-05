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
    inject,
} from '@angular/core';
import {
    ActionbarComponent,
    CombinedRenderData,
    DefaultGroups,
    ElementType,
    EduSharingUiModule,
    OptionItem,
    OptionsHelperDataService,
    RenderHelperService,
    Scope,
    TranslationsService,
} from 'ngx-edu-sharing-ui';

import { MatButtonModule } from '@angular/material/button';
import { RenderComponent, RenderingServiceLibModule } from 'ngx-rendering-service-lib';
import { MdsModule } from '../../../features/mds/mds.module';
import { SharedModule } from '../../../shared/shared.module';
import { HOME_REPOSITORY, Node, NodeService, RestConstants } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { NodeHelperService } from '../../../services/node-helper.service';

@Component({
    selector: 'es-render-wrapper-component',
    templateUrl: 'render-wrapper.component.html',
    styleUrls: ['render-wrapper.component.scss'],
    imports: [
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
    private renderHelperService = inject(RenderHelperService);
    private nodeService = inject(NodeService);
    private nodeHelper = inject(NodeHelperService);
    private translations = inject(TranslationsService);
    private optionsHelper = inject(OptionsHelperDataService);

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
    private parentNode = signal<Node>(null);

    constructor() {
        this.translations.waitForInit().subscribe(() => {});
        this.optionsHelper.registerGlobalKeyboardShortcuts();
        this.data.set(undefined);
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.nodeId) {
            if (this.showChildobjects) {
                this.parentNode.set(
                    await firstValueFrom(
                        this.nodeService.getNode(changes.nodeId.currentValue, {
                            repository: this.repository || HOME_REPOSITORY,
                        }),
                    ),
                );
                try {
                    this.children.set(
                        (
                            await firstValueFrom(
                                this.nodeService.getChildren(changes.nodeId.currentValue, {
                                    repository: this.repository || HOME_REPOSITORY,
                                    filter: ['files'],
                                    sortProperties: [RestConstants.CCM_PROP_CHILDOBJECT_ORDER],
                                    sortAscending: [true],
                                    assocName: RestConstants.CCM_ASSOC_CHILDIO,
                                    maxItems: RestConstants.COUNT_UNLIMITED,
                                }),
                            )
                        ).nodes,
                    );
                } catch (e) {
                    this.children.set(null);
                    e.preventDefault();
                    console.warn('Could not fetch children', e);
                }
            } else {
                this.children.set(null);
            }
        }
        if (changes.nodeId || changes.childId) {
            await this.refresh();
        }
    }

    /**
     * zip download btn (series object)
     * @param node
     * @private
     */
    private addDownloadAllBtn(node: Node) {
        const children = this.children();
        if (!node || !children?.length) {
            return;
        }
        const parent = this.parentNode();
        const downloadAll = new OptionItem('OPTIONS.DOWNLOAD_ALL', 'archive', () => {
            void this.nodeHelper.downloadNodes([parent].concat(children), parent.name + '.zip');
        });
        downloadAll.elementType = [
            ElementType.Node,
            ElementType.NodeChild,
            ElementType.NodePublishedCopy,
        ];
        downloadAll.group = DefaultGroups.View;
        downloadAll.priority = 35;
        this.optionsHelper.setData({
            scope: Scope.Render,
            activeObjects: [node],
            parent: { ref: { id: node.parent.id } },
            customOptions: {
                useDefaultOptions: true,
                addOptions: [downloadAll],
            },
        });
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
        this.addDownloadAllBtn(data.node);
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
