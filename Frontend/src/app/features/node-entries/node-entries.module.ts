import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { ListItemsModule } from '../list-items/list-items.module';
import { DragPreviewComponent } from './drag-preview/drag-preview.component';
import { FocusStateDirective } from './focus-state.directive';
import { ListItemLabelPipe } from './list-item-label.pipe';
import { NodeEntriesCardGridComponent } from './node-entries-card-grid/node-entries-card-grid.component';
import { NodeEntriesCardSmallComponent } from './node-entries-card-small/node-entries-card-small.component';
import { NodeEntriesCardComponent } from './node-entries-card/node-entries-card.component';
import { NodeEntriesGlobalOptionsComponent } from './node-entries-global-options/node-entries-global-options.component';
import { ColumnChooserComponent } from './node-entries-table/column-chooser/column-chooser.component';
import { NodeEntriesTableComponent } from './node-entries-table/node-entries-table.component';
import { NodeEntriesWrapperComponent } from './node-entries-wrapper.component';
import { NodeEntriesComponent } from './node-entries.component';
import { NodeRatingComponent } from './node-rating/node-rating.component';
import { NodeStatsBadgesComponent } from './node-stats-badges/node-stats-badges.component';
import { NodeTypeBadgeComponent } from './node-type-badge/node-type-badge.component';
import { OptionButtonComponent } from './option-button/option-button.component';
import { PreviewImageComponent } from './preview-image/preview-image.component';
import { SortSelectPanelComponent } from './sort-select-panel/sort-select-panel.component';

@NgModule({
    declarations: [
        ColumnChooserComponent,
        DragPreviewComponent,
        FocusStateDirective,
        ListItemLabelPipe,
        NodeEntriesCardComponent,
        NodeEntriesCardGridComponent,
        NodeEntriesCardSmallComponent,
        NodeEntriesComponent,
        NodeEntriesGlobalOptionsComponent,
        NodeEntriesTableComponent,
        NodeEntriesWrapperComponent,
        NodeRatingComponent,
        NodeTypeBadgeComponent,
        OptionButtonComponent,
        PreviewImageComponent,
        SortSelectPanelComponent,
        NodeStatsBadgesComponent,
    ],
    imports: [SharedModule, ListItemsModule],
    exports: [
        ListItemLabelPipe,
        NodeEntriesWrapperComponent,
        NodeStatsBadgesComponent,
        NodeTypeBadgeComponent,
    ],
})
export class NodeEntriesModule {}
