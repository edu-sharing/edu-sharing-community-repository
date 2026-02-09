import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {
    Component,
    EventEmitter,
    input,
    Input,
    InputSignal,
    Output,
    signal,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { SharedModule } from '../../../../shared/shared.module';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { CollectionListDisplayType } from '../../shared/types/collection-list-display-type';
import { LayoutOption } from '../../shared/types/layout-option';
import { CollectionChipsConfig } from '../../shared/types/widget-config/collection-chips-config';
import { WidgetConfigurationButtonsComponent } from '../shared/widget-configuration-buttons/widget-configuration-buttons.component';
import { ConfigurationOption } from '../../shared/types/configuration-option';

@Component({
    selector: 'es-collection-chips',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [EduSharingUiCommonModule, SharedModule, WidgetConfigurationButtonsComponent],
    templateUrl: './collection-chips.component.html',
    styleUrls: ['./collection-chips.component.scss'],
})
export class CollectionChipsComponent {
    // INPUTS + OUTPUTS
    @Input() contextNodeId!: string;
    @Input() customUrl?: (collection: Node) => string;
    editMode: InputSignal<boolean> = input<boolean>(false);
    @Input() embedConfigurationOption?: ConfigurationOption;
    @Input() gridIndex: number = -1;
    @Input() pageVariantNode?: Node;
    searchInput: InputSignal<string> = input<string>(null);
    @Input() swimlaneIndex: number = -1;

    @Output() configChanged: EventEmitter<void> = new EventEmitter<void>();
    @Output() embedWidgetClicked: EventEmitter<void> = new EventEmitter<void>();

    // VARIABLES
    layout: CollectionListDisplayType = CollectionListDisplayType.Chips;
    layoutOptions: LayoutOption[] = [
        {
            ariaLabel: 'BUTTONS_ARIA',
            icon: 'svg-view_pills',
            value: CollectionListDisplayType.Chips,
            viewValue: 'BUTTONS',
        },
        {
            ariaLabel: 'GRID_ARIA',
            icon: 'view_module',
            value: CollectionListDisplayType.Tiles,
            viewValue: 'GRID',
        },
    ];
    dragging: boolean = false;
    initialized: WritableSignal<boolean> = signal(false);
    list: Node[];
    updateInProgress: WritableSignal<boolean> = signal(false);

    constructor(private topicPageHelperService: TopicPageHelperService) {}

    /**
     * Opens the link to a collection.
     * Note: The click event is necessary, as drag-and-drop does not work with href.
     *
     * @param node
     */
    collectionItemClicked(node: Node): void {
        if (this.dragging) {
            return;
        }
        const url: string =
            this.customUrl && this.customUrl(node)
                ? this.customUrl(node)
                : node.properties[RestConstants.LOM_PROP_TECHNICAL_LOCATION]?.[0];
        if (url) {
            window.open(url, '_self');
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
        // load the sub-collections, filter them by editorial state and sort by name and ccm:collection_ordered_position afterward
        this.list = (await this.topicPageHelperService.getSubCollections(this.contextNodeId))
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
            this.list.sort(
                (a: Node, b: Node) =>
                    config.sortedNodeIds.indexOf(a.ref.id) - config.sortedNodeIds.indexOf(b.ref.id),
            );
        }
    }

    protected readonly CollectionListLayout = CollectionListDisplayType;
}
