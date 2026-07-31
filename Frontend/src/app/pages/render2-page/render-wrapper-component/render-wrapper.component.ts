import {
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    inject,
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
    DefaultGroups,
    EduSharingUiModule,
    ElementType,
    OptionItem,
    OptionsHelperDataService,
    RenderHelperService,
    Scope,
    TranslationsService,
} from 'ngx-edu-sharing-ui';

import { MatButtonModule } from '@angular/material/button';
import { RenderComponent, RenderData, RenderingServiceLibModule } from 'ngx-rendering-service-lib';
import { MdsModule } from '../../../features/mds/mds.module';
import { SharedModule } from '../../../shared/shared.module';
import { HOME_REPOSITORY, Node, NodeService, RestConstants } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { NodeHelperService } from '../../../services/node-helper.service';
import { EditorialSidebarService } from '../../../features/editorial-sidebar/editorial-sidebar.service';
import { provideReusableOptionsHelperData } from '../../../services/options-helper-data.provider';

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
    providers: [provideReusableOptionsHelperData(), RenderHelperService],
})
export class RenderWrapperComponent implements OnChanges {
    private renderHelperService = inject(RenderHelperService);
    private nodeService = inject(NodeService);
    private nodeHelper = inject(NodeHelperService);
    private translations = inject(TranslationsService);
    private optionsHelper = inject(OptionsHelperDataService);
    private editorialSidebarService = inject(EditorialSidebarService);

    @ViewChild(ActionbarComponent) actionbar: ActionbarComponent;
    @ViewChild(RenderComponent) private renderComponent?: RenderComponent;
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
    /**
     * if set, modules ignore their per-type width settings and render full width (edge-to-edge)
     */
    @HostBinding('class.full-width') @Input() fullWidth = false;
    @Output() childIdChange = new EventEmitter<string>();
    @Output() closeClick = new EventEmitter<void>();
    /** the currently rendered node, so a host (e.g. render2-page) can drive its editorial sidebar */
    @Output() nodeChange = new EventEmitter<Node>();

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
        const addOptions = [];
        if (node && children?.length) {
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
            addOptions.push(downloadAll);
        }
        this.optionsHelper.setData({
            scope: Scope.Render,
            activeObjects: [node],
            selectedObjects: [node],
            allObjects: [node],
            parent: { ref: { id: node.parent.id } },
            customOptions: {
                useDefaultOptions: true,
                addOptions,
            },
            postPrepareOptions: (o) => {
                const isSodix =
                    node?.properties?.['ccm:replicationsource']?.[0]?.toLowerCase() === 'sodix';
                o.filter((o) => o.name === 'OPTIONS.DOWNLOAD').forEach((download) => {
                    download.showAsAction = true;
                    // Only Sodix links expire and are refreshable. Leave every other node's default
                    // download (and its show condition) untouched — fetching would 400 on a
                    // non-refreshable module and error out the render.
                    if (!isSodix) {
                        return;
                    }
                    // Deferred Sodix nodes have no url up front, so force the button visible...
                    download.customShowCallback = async () => true;
                    // ...and fetch a fresh (non-expired) url on every click before downloading it.
                    download.callback = async () => {
                        const data = await this.fetchLinks();
                        const url = data?.items?.[0]?.additionalData?.['downloadUrl'];
                        if (url) {
                            this.nodeHelper.downloadUrl(url, 'download', {
                                node,
                                triggerTrackingEvent: true,
                            });
                        } else {
                            void this.nodeHelper.downloadNode(node);
                        }
                    };
                });
            },
        });
    }
    async refresh() {
        await this.setNodeById(this.childId || this.nodeId);
    }

    /**
     * Request fresh render links for the currently displayed node without a full re-render
     * (e.g. when a short-lived link such as a Sodix playout URL has expired). Delegates to the
     * embedded rendering component, which re-fetches via the backend and swaps in the new link.
     */
    reloadLinks(): void {
        void this.renderComponent?.reloadLinks();
    }

    /**
     * Fetch fresh render links on demand for the currently displayed node and resolve once they
     * arrive — e.g. to refresh an expired Sodix download/playout URL before acting on it.
     * `items[0].link` is the playout URL, `items[0].additionalData.downloadUrl` the download URL.
     * Returns null if no rendering component is mounted yet.
     */
    fetchLinks(): Promise<RenderData | null> {
        return this.renderComponent?.fetchLinks() ?? Promise.resolve(null);
    }

    private async setNodeById(nodeId: string) {
        this.loading.set(true);
        delete this.data()?.request;
        this.data.set(this.data());
        this.optionsHelper;
        const data = await this.renderHelperService.getRenderData(
            nodeId,
            this.version,
            this.repository,
        );
        // register currently rendered node for sidebar interactions (incl. download-all btn + postPrepareOptions)
        this.addDownloadAllBtn(data.node);
        // surface the rendered node so a host can populate its editorial sidebar options
        this.nodeChange.emit(data.node);
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
        this.closeClick.emit();
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
