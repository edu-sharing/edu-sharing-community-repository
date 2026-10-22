import {
    AfterViewChecked,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    inject,
} from '@angular/core';
import { MdsWidget, Node, NodeEntries } from 'ngx-edu-sharing-api';
import { Values } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../../shared/shared.module';
import { WIDGET_TYPE_OPTIONS, WIDGETS } from '../../shared/types/custom-definitions';
import { GridTile } from '../../shared/types/grid-tile';
import { GridTileToHitsMapping } from '../../shared/types/grid-tile-to-hits-mapping';
import { GridTileToSearchCountMapping } from '../../shared/types/grid-tile-to-search-count-mapping';
import { GridTileToSearchResultsMapping } from '../../shared/types/grid-tile-to-search-results-mapping';
import { SwimlaneBackgroundShape } from '../../shared/types/swimlane-background-shape';
import { convertNodeRefIntoNodeId } from '../../shared/utils/template-util';
import { GenericWidgetComponent } from '../../widgets/generic-widget/generic-widget.component';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';
import { ConfigureGridComponent } from './configure-grid/configure-grid.component';
import { SelectWidgetTypeComponent } from './select-widget-type/select-widget-type.component';

@Component({
    selector: 'es-swimlane',
    imports: [
        SharedModule,
        ConfigureGridComponent,
        GenericWidgetComponent,
        SelectWidgetTypeComponent,
    ],
    templateUrl: './swimlane.component.html',
    styleUrls: ['./swimlane.component.scss'],
})
export class SwimlaneComponent implements AfterViewChecked, OnChanges {
    private cdr = inject(ChangeDetectorRef);
    private elementRef = inject(ElementRef);
    private genericWidgetGlobalService = inject(GenericWidgetGlobalService);

    @Input() set aiSupported(value: boolean) {
        if (!value) {
            this.supportedWidgetTypes = this.supportedWidgetTypes.filter(
                (widgetType) => widgetType !== WIDGETS.AI_TEXT_WIDGET,
            );
        }
    }
    @Input() backgroundColor?: string;
    @Input() backgroundShape?: SwimlaneBackgroundShape = SwimlaneBackgroundShape.None;
    @Input() contextNodeId: string;
    @Input() editMode: boolean;
    @Input() grid: GridTile[] = [];
    @Input() pageVariantNode?: Node;
    @Input() set rendering2Supported(value: boolean) {
        if (!value) {
            this.supportedWidgetTypes = this.supportedWidgetTypes.filter(
                (widgetType) => widgetType !== WIDGETS.MEDIA_RENDERING,
            );
        }
    }
    @Input() searchInput: string;
    @Input() searchFilters: Values;
    @Input() selectDimensions: Map<string, MdsWidget> = new Map<string, MdsWidget>();
    @Input() swimlaneIndex: number;
    @Input() topicWidgets: NodeEntries;
    @Output() gridUpdated: EventEmitter<GridTile[]> = new EventEmitter<GridTile[]>();
    @Output() searchHitsChanged: EventEmitter<GridTileToHitsMapping> =
        new EventEmitter<GridTileToHitsMapping>();
    @Output() totalSearchResultCountChanged: EventEmitter<GridTileToSearchCountMapping> =
        new EventEmitter<GridTileToSearchCountMapping>();
    @Output() visibleNodesChanged: EventEmitter<GridTileToSearchResultsMapping> =
        new EventEmitter<GridTileToSearchResultsMapping>();

    supportedWidgetTypes: string[] = WIDGET_TYPE_OPTIONS.map((option) => option.value);
    swimlaneColor: string;

    /**
     * Reports widget types this installation cannot render; readers only see an empty tile.
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (!changes.grid) {
            return;
        }
        (this.grid ?? [])
            .filter((tile: GridTile) => tile.item && !this.isSupportedWidget(tile.item))
            .forEach((tile: GridTile) =>
                console.warn('Unsupported topic page widget type, tile left empty:', tile.item),
            );
    }

    /**
     * After the view was checked, retrieve the swimlane color from the computed styles
     */
    ngAfterViewChecked(): void {
        const swimlaneParent = this.elementRef.nativeElement?.offsetParent;
        if (
            swimlaneParent &&
            getComputedStyle(swimlaneParent).getPropertyValue('background-color') &&
            !this.swimlaneColor
        ) {
            this.swimlaneColor =
                getComputedStyle(swimlaneParent).getPropertyValue('background-color');
            // it seems like the change detection is not happening automatically
            // related issue: https://stackoverflow.com/a/45300527
            this.cdr.detectChanges();
        }
    }

    /**
     * Called by es-generic-widget visibleNodesChanged output event.
     * Emits the visible nodes.
     */
    changeVisibleNodes(nodes: Node[], gridIndex: number): void {
        this.visibleNodesChanged.emit({ gridIndex, nodes });
    }

    /**
     * Called by es-generic-widget totalSearchResultCountChanged output event.
     * Emits the total search result count.
     */
    changeTotalSearchResultCount(count: number, gridIndex: number): void {
        this.totalSearchResultCountChanged.emit({ gridIndex, count });
    }

    /**
     * Called by es-generic-widget searchHitsChanged output event.
     * Emits the hasHits.
     */
    changeSearchHits(hasHits: boolean, gridIndex: number): void {
        this.searchHitsChanged.emit({ hasHits, gridIndex });
    }

    /**
     * Called by es-configure-grid and es-select-widget-type-dialog gridUpdated output event.
     * Emits the updated grid.
     */
    updatedGrid(grid: GridTile[]): void {
        this.gridUpdated.emit(grid);
    }

    /**
     * Checks whether a given widget type name is a supported widget type.
     */
    isSupportedWidget(name: string) {
        return (
            this.supportedWidgetTypes.includes(name) ||
            this.genericWidgetGlobalService.hasCustomWidget(name)
        );
    }

    protected readonly convertNodeRefIntoNodeId = convertNodeRefIntoNodeId;
}
