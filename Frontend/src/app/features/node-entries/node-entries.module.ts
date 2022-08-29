import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
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
import { OptionButtonComponent } from './option-button/option-button.component';
import { PreviewImageComponent } from './preview-image/preview-image.component';
import { DragPreviewComponent } from './drag-preview/drag-preview.component';
import { NodeEntriesGlobalService } from './node-entries-global.service';

@NgModule({
    declarations: [
        NodeEntriesCardComponent,
        NodeEntriesCardGridComponent,
        NodeEntriesCardSmallComponent,
        NodeEntriesTableComponent,
        NodeRatingComponent,
        PreviewImageComponent,
        NodeEntriesWrapperComponent,
        NodeEntriesComponent,
        ColumnChooserComponent,
        ListItemLabelPipe,
        OptionButtonComponent,
        FocusStateDirective,
        DragPreviewComponent,
    ],
    imports: [SharedModule, ListItemsModule],
    providers: [NodeEntriesGlobalService],
    exports: [NodeEntriesWrapperComponent, ListItemLabelPipe],
})
export class NodeEntriesModule {}
