import { A11yModule } from '@angular/cdk/a11y';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRippleModule } from '@angular/material/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { NgxSliderModule } from '@angular-slider/ngx-slider';
import { BreadcrumbsComponent } from './components/breadcrumbs/breadcrumbs.component';
import { CollectionChooserComponent } from './components/collection-chooser/collection-chooser.component';
import { CustomNodeListWrapperComponent } from './components/custom-node-list-wrapper/custom-node-list-wrapper.component';
import { InputPasswordComponent } from './components/input-password/input-password.component';
import { ListOptionItemComponent } from './components/list-option-item/list-option-item.component';
import { ListTableComponent } from './components/list-table/list-table.component';
import { ListBaseComponent } from './components/list-table/widgets/list-base/list-base.component';
import { ListCollectionInfoComponent } from './components/list-table/widgets/list-collection-info/list-collection-info.component';
import { ListCountsComponent } from './components/list-table/widgets/list-counts/list-counts.component';
import { ListNodeLicenseComponent } from './components/list-table/widgets/list-node-license/list-node-license.component';
import { ListNodeReplicationSourceComponent } from './components/list-table/widgets/list-node-replication-source/list-node-replication-source.component';
import { ListNodeWorkflowComponent } from './components/list-table/widgets/list-node-workflow/list-node-workflow.component';
import { ListTextComponent } from './components/list-table/widgets/list-text/list-text.component';
import { NodeUrlComponent } from './components/node-url/node-url.component';
import { SortDropdownComponent } from './components/sort-dropdown/sort-dropdown.component';
import { UserTileComponent } from './components/user-tile/user-tile.component';
import { DurationPipe } from './components/video-controls/duration.pipe';
import { VideoControlsComponent } from './components/video-controls/video-controls.component';
import { DistinctClickDirective } from './directives/distinct-click.directive';
import { NodesDragSourceDirective } from './directives/drag-nodes/nodes-drag-source.directive';
import { NodesDropTargetDirective } from './directives/drag-nodes/nodes-drop-target.directive';
import { TitleDirective } from './directives/title.directive';
import { NodeHelperService } from './node-helper.service';
import { OptionsHelperService } from './options-helper.service';
import { NodeDatePipe } from './pipes/date.pipe';
import { FormatSizePipe } from './pipes/file-size.pipe';
import { KeysPipe } from './pipes/keys.pipe';
import { NodeImageSizePipe } from './pipes/node-image-size.pipe';
import { NodePersonNamePipe } from './pipes/node-person-name.pipe';
import { NodeSourcePipe } from './pipes/node-source.pipe';
import { PermissionNamePipe } from './pipes/permission-name.pipe';
import { SafeHtmlPipe } from './pipes/safe-html.pipe';
import { UrlPipe } from './pipes/url.pipe';
import { Toast } from './toast';
import {ImageConfigDirective} from './directives/image-config.directive';
import {ErrorProcessingService} from './error.processing';
import {ToastMessageComponent} from './components/toast-message/toast-message.component';
import { FormatDurationPipe } from './pipes/format-duration.pipe';
import {RenderHelperService} from './render-helper.service';
import {DragDropModule} from '@angular/cdk/drag-drop';
import {NodeEntriesComponent} from './components/node-entries/node-entries.component';
import {NodeEntriesWrapperComponent} from './components/node-entries-wrapper/node-entries-wrapper.component';
import {NodeEntriesCardGridComponent} from './components/node-entries/node-entries-card-grid/node-entries-card-grid.component';
import {NodeEntriesCardComponent} from './components/node-entries/node-entries-card/node-entries-card.component';
import {NodeImagePipe} from './pipes/node-image.pipe';
import {NodeTitlePipe} from './pipes/node-title.pipe';
import {OptionButtonComponent} from './components/option-button/option-button.component';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {NodeEntriesDragDirective} from './directives/node-entries-drag';
import {NodeEntriesCardSmallComponent} from './components/node-entries/node-entries-card-small/node-entries-card-small.component';
import {NodeEntriesTableComponent} from './components/node-entries/node-entries-table/node-entries-table.component';
import {MatTableModule} from '@angular/material/table';
import {ColumnChooserComponent} from './components/node-entries/node-entries-table/column-chooser/column-chooser.component';
import {MatPaginatorModule} from '@angular/material/paginator';
import {MatSortModule} from '@angular/material/sort';
import {OverlayModule} from '@angular/cdk/overlay';
import {ListItemLabelPipe} from './pipes/list-item-label.pipe';
import {DragCursorDirective} from './directives/drag-cursor.directive';
import {NodeRatingComponent} from './components/node-entries/node-rating/node-rating.component';
import { PreviewImageComponent } from './components/node-entries/preview-image/preview-image.component';
import { FocusStateDirective } from './directives/focus-state.directive';
import { SharedModule } from '../shared/shared.module';

@NgModule({
    declarations: [
        CollectionChooserComponent,
        ListTableComponent,
        NodeEntriesComponent,
        ListBaseComponent,
        ListCollectionInfoComponent,
        ListNodeLicenseComponent,
        ListNodeReplicationSourceComponent,
        ListNodeWorkflowComponent,
        ListTextComponent,
        ListCountsComponent,
        NodeEntriesDragDirective,
        SortDropdownComponent,
        UserTileComponent,
        CustomNodeListWrapperComponent,
        BreadcrumbsComponent,
        VideoControlsComponent,
        OptionButtonComponent,
        InputPasswordComponent,
        NodePersonNamePipe,
        NodeDatePipe,
        NodeUrlComponent,
        ToastMessageComponent,
        FormatSizePipe,
        KeysPipe,
        PermissionNamePipe,
        NodeImageSizePipe,
        UrlPipe,
        NodeImagePipe,
        NodeSourcePipe,
        NodeTitlePipe,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
        ImageConfigDirective,
        SafeHtmlPipe,
        ListOptionItemComponent,
        DistinctClickDirective,
        DurationPipe,
        TitleDirective,
        FormatDurationPipe,
        NodeEntriesWrapperComponent,
        NodeEntriesComponent,
        NodeEntriesCardGridComponent,
        NodeEntriesCardComponent,
        NodeRatingComponent,
        NodeEntriesCardSmallComponent,
        NodeEntriesTableComponent,
        ColumnChooserComponent,
        ListItemLabelPipe,
        DragCursorDirective,
        PreviewImageComponent,
        FocusStateDirective,
    ],
    imports: [
        SharedModule,
        A11yModule,
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        DragDropModule,
        MatButtonModule,
        MatCardModule,
        MatCheckboxModule,
        MatExpansionModule,
        MatInputModule,
        MatMenuModule,
        MatProgressBarModule,
        MatProgressSpinnerModule,
        MatRadioModule,
        MatRippleModule,
        MatSnackBarModule,
        MatTabsModule,
        MatTooltipModule,
        NgxSliderModule,
        RouterModule,
        MatSlideToggleModule,
        MatTableModule,
        MatPaginatorModule,
        MatSortModule,
        OverlayModule,
    ],
    providers: [
        Toast,
        ErrorProcessingService,
        NodeHelperService,
        RenderHelperService,
        OptionsHelperService],
    exports: [
        SharedModule,
        ListTableComponent,
        NodeEntriesComponent,
        CustomNodeListWrapperComponent,
        BreadcrumbsComponent,
        ListOptionItemComponent,
        InputPasswordComponent,
        VideoControlsComponent,
        ImageConfigDirective,
        UserTileComponent,
        CollectionChooserComponent,
        SortDropdownComponent,
        NodeDatePipe,
        NodeUrlComponent,
        FormatSizePipe,
        KeysPipe,
        PermissionNamePipe,
        UrlPipe,
        ListItemLabelPipe,
        NodeSourcePipe,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
        ListCollectionInfoComponent,
        ListBaseComponent,
        ListTextComponent,
        NodeTitlePipe,
        TitleDirective,
        SafeHtmlPipe,
        NodeEntriesWrapperComponent,
        DragCursorDirective,
        NodeEntriesTableComponent,
    ],
})
export class CoreUiModule {}
