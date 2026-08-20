import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {
    Component,
    computed,
    EventEmitter,
    input,
    Input,
    InputSignal,
    Output,
    Signal,
    signal,
    ViewEncapsulation,
    WritableSignal,
    inject,
} from '@angular/core';
import { Router } from '@angular/router';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, NodeHelperService } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { SharedModule } from '../../../../shared/shared.module';
import { TopicPageGlobalService } from '../../shared/services/topic-page-global.service';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { CollectionListDisplayType } from '../../shared/types/collection-list-display-type';
import { ConfigurationOption } from '../../shared/types/configuration-option';
import { LayoutOption } from '../../shared/types/layout-option';
import { CollectionChipsConfig } from '../../shared/types/widget-config/collection-chips-config';
import { WidgetComponentInterface } from '../generic-widget/generic-widget.component';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';

@Component({
    selector: 'es-collection-chips',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [EduSharingUiCommonModule, SharedModule, WidgetConfigurationButtonsComponent],
    templateUrl: './collection-chips.component.html',
    styleUrls: ['./collection-chips.component.scss'],
})
export class CollectionChipsComponent implements WidgetComponentInterface {
    private nodeHelper = inject(NodeHelperService);
    private router = inject(Router);
    private topicPageGlobalService = inject(TopicPageGlobalService);
    private topicPageHelperService = inject(TopicPageHelperService);

    protected readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.COLLECTION_CHIPS.';

    // INPUTS + OUTPUTS
    @Input() contextNodeId!: string;
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() embedConfigurationOption?: ConfigurationOption;
    @Input() gridIndex: number = -1;
    @Input() displayLimit: number = 0;
    @Input() pageVariantNode?: Node;
    searchInput: InputSignal<string> = input<string>(null);
    @Input() swimlaneIndex: number = -1;

    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();
    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();

    // VARIABLES
    customCollectionChipsTileColor: string;
    customUrl: (node: Node) => string;
    layout: CollectionListDisplayType = CollectionListDisplayType.Chips;
    layoutOptions: LayoutOption[] = [
        {
            ariaLabel: 'BUTTONS_ARIA',
            icon: 'edu-view_pills',
            value: CollectionListDisplayType.Chips,
            viewValue: 'BUTTONS',
        },
        {
            ariaLabel: 'GRID_ARIA',
            icon: 'grid_view',
            value: CollectionListDisplayType.Tiles,
            viewValue: 'GRID',
        },
    ];
    dragging: boolean = false;
    initialized: WritableSignal<boolean> = signal(false);
    list: Node[] = [];
    updateInProgress: WritableSignal<boolean> = signal(false);
    showMore: WritableSignal<boolean> = signal(false);

    protected readonly visibleList: Signal<Node[]> = computed(() =>
        this.editMode() || this.showMore() || !this.displayLimit
            ? this.list ?? []
            : (this.list ?? []).slice(0, this.displayLimit),
    );

    constructor() {
        if (this.topicPageGlobalService.getCustomUrlFunction()) {
            this.customUrl = this.topicPageGlobalService.getCustomUrlFunction();
        }
        this.customCollectionChipsTileColor =
            this.topicPageGlobalService.getCustomCollectionChipsTileColor();
    }

    /**
     * Opens the link to a collection.
     * Note: The click event is necessary, as drag-and-drop does not work with href.
     *
     * @param node
     */
    async collectionItemClicked(node: Node): Promise<void> {
        if (this.dragging) {
            return;
        }
        let url: string = this.customUrl && this.customUrl(node) ? this.customUrl(node) : null;
        if (!url) {
            const queryParamsArray = Object.entries(
                this.nodeHelper.getNodeLink('queryParams', node),
            )
                .filter((k) => !!k[1] && k[0] !== 'scope')
                .map((k) => k[0] + '=' + encodeURIComponent(k[1]));
            url =
                (this.nodeHelper.getNodeLink('routerLink', node) as string) +
                (queryParamsArray.length > 0 ? '?' + queryParamsArray.join('&') : '');
            await this.router.navigateByUrl(url);
        } else {
            window.open(url, this.topicPageGlobalService.getCustomUrlTarget());
        }
    }

    /**
     * Reacts to an item being dropped.
     *
     * @param event
     */
    async drop(event: CdkDragDrop<string[]>): Promise<void> {
        moveItemInArray(this.list, event.previousIndex, event.currentIndex);
        this.configChanged.emit();
    }

    /**
     * Handles the change of the layout by emitting a config changed event.
     */
    changeLayout(): void {
        this.configChanged.emit();
    }

    /**
     * Handles the embedding of the widget by emitting an embed widget clicked event.
     */
    embedWidget(): void {
        this.embedWidgetClicked.emit();
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Preloads and processes the list of sub-collections.
     */
    async preLoadAction(): Promise<void> {
        if (!this.contextNodeId) {
            this.list = [];
            return;
        }
        let subCollections: Node[];
        try {
            subCollections = await this.topicPageHelperService.getSubCollections(
                this.contextNodeId,
            );
        } catch {
            // children may be inaccessible (e.g. missing read permission) - render nothing
            this.list = [];
            return;
        }
        // filter them by editorial state and sort by name and ccm:collection_ordered_position afterward
        this.list = subCollections
            .filter(
                (c: Node) =>
                    !c.properties[RestConstants.CCM_PROP_IO_EDITORIAL_STATE]?.includes(
                        'deactivated',
                    ),
            )
            .sort((a: Node, b: Node) => a.name.localeCompare(b.name))
            .sort((a: Node, b: Node): number => {
                const aPos: string =
                    a.properties?.[RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION]?.[0];
                const bPos: string =
                    b.properties?.[RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION]?.[0];
                if (aPos && bPos) {
                    return parseInt(aPos) - parseInt(bPos);
                }
                return 0;
            });
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to retrieve a widget config from the currently set variables in the component.
     */
    retrieveWidgetConfig(): CollectionChipsConfig {
        return {
            collectionListLayout: this.layout,
            sortedNodeIds: this.list.map((node: Node) => node.ref.id),
        };
    }

    // noinspection JSUnusedGlobalSymbols
    /**
     * Called by generic-widget component to set widget-specific values.
     *
     * @param config
     */
    setWidgetValues(config: CollectionChipsConfig): void {
        // 0 is a valid enum value, so check for undefined
        if (config.collectionListLayout !== undefined) {
            this.layout = config.collectionListLayout;
        }
        if (config.sortedNodeIds?.length) {
            this.list.sort((a: Node, b: Node) => {
                // check whether the node is in the list of sorted node ids,
                // if not, keep the existing order and put it at the end of the list
                const indexA = config.sortedNodeIds.indexOf(a.ref.id);
                const indexB = config.sortedNodeIds.indexOf(b.ref.id);

                const sortedIndexA = indexA === -1 ? Number.MAX_SAFE_INTEGER : indexA;
                const sortedIndexB = indexB === -1 ? Number.MAX_SAFE_INTEGER : indexB;

                return sortedIndexA - sortedIndexB;
            });
        }
    }

    /**
     * Toggles the show more state.
     */
    toggleShowMore(): void {
        this.showMore.set(!this.showMore());
    }

    protected readonly CollectionListLayout = CollectionListDisplayType;
}
