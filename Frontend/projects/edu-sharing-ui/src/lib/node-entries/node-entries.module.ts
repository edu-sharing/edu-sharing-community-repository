import { NgModule } from '@angular/core';
import { ListItemsModule } from '../list-items/list-items.module';
import { FocusStateDirective } from './focus-state.directive';
import { ListItemLabelPipe } from './list-item-label.pipe';
import { NodeEntriesCardGridComponent } from './node-entries-card-grid/node-entries-card-grid.component';
import { NodeEntriesCardSmallComponent } from './node-entries-card-small/node-entries-card-small.component';
import { NodeEntriesCardComponent } from './node-entries-card/node-entries-card.component';
import { ColumnChooserComponent } from './node-entries-table/column-chooser/column-chooser.component';
import { NodeEntriesTableComponent } from './node-entries-table/node-entries-table.component';
import { NodeEntriesWrapperComponent } from './node-entries-wrapper.component';
import { NodeEntriesComponent } from './node-entries.component';
import { NodeRatingComponent } from './node-rating/node-rating.component';
import { NodeStatsBadgesComponent } from './node-stats-badges/node-stats-badges.component';
import { NodeTypeBadgeComponent } from './node-type-badge/node-type-badge.component';
import { OptionButtonComponent } from './option-button/option-button.component';
import { PreviewImageComponent } from './preview-image/preview-image.component';
import { DragPreviewComponent } from './drag-preview/drag-preview.component';
import { SortSelectPanelComponent } from './sort-select-panel/sort-select-panel.component';
import { EduSharingUiCommonModule } from '../common/edu-sharing-ui-common.module';
import { MatBadgeModule } from '@angular/material/badge';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { MatPaginatorModule } from '@angular/material/paginator';
import { OverlayModule } from '@angular/cdk/overlay';
import { MatMenuModule } from '@angular/material/menu';
import { NodesDragDirective } from '../directives/drag-nodes/nodes-drag.directive';
import { NodesDragSourceDirective } from '../directives/drag-nodes/nodes-drag-source.directive';
import { NodesDropTargetDirective } from '../directives/drag-nodes/nodes-drop-target.directive';
import { MatTableModule } from '@angular/material/table';
import { A11yModule } from '@angular/cdk/a11y';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSortModule } from '@angular/material/sort';
import { MatRippleModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { NodeEntriesGlobalOptionsComponent } from './node-entries-global-options/node-entries-global-options.component';

@NgModule({
    declarations: [
        ColumnChooserComponent,
        DragPreviewComponent,
        FocusStateDirective,
        ListItemLabelPipe,
        NodeEntriesCardComponent,
        NodeEntriesCardGridComponent,
        NodeEntriesCardSmallComponent,
        NodeEntriesTableComponent,
        NodeRatingComponent,
        PreviewImageComponent,
        NodeEntriesComponent,
        NodeEntriesWrapperComponent,
        NodeRatingComponent,
        NodeTypeBadgeComponent,
        OptionButtonComponent,
        PreviewImageComponent,
        SortSelectPanelComponent,
        NodesDragDirective,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
        NodeEntriesGlobalOptionsComponent,
        NodeStatsBadgesComponent,
    ],
    imports: [
        CommonModule,
        FormsModule,
        A11yModule,
        OverlayModule,
        DragDropModule,
        EduSharingUiCommonModule,
        ListItemsModule,
        MatCheckboxModule,
        MatButtonModule,
        MatBadgeModule,
        MatMenuModule,
        MatTableModule,
        MatCheckboxModule,
        MatPaginatorModule,
        MatRippleModule,
        MatSortModule,
        MatTooltipModule,
        TranslateModule,
    ],
    exports: [
        NodeEntriesWrapperComponent,
        NodesDragDirective,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
        ListItemLabelPipe,
    ],
})
export class NodeEntriesModule {}
