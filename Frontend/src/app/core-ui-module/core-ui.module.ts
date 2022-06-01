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
import { CollectionChooserComponent } from './components/collection-chooser/collection-chooser.component';
import { CustomNodeListWrapperComponent } from './components/custom-node-list-wrapper/custom-node-list-wrapper.component';
import { InputPasswordComponent } from './components/input-password/input-password.component';
import { ListOptionItemComponent } from './components/list-option-item/list-option-item.component';
import { ListTableComponent } from './components/list-table/list-table.component';
import { UserTileComponent } from './components/user-tile/user-tile.component';
import { DurationPipe } from './components/video-controls/duration.pipe';
import { VideoControlsComponent } from './components/video-controls/video-controls.component';
import { DistinctClickDirective } from './directives/distinct-click.directive';
import { NodesDragSourceDirective } from './directives/drag-nodes/nodes-drag-source.directive';
import { NodesDropTargetDirective } from './directives/drag-nodes/nodes-drop-target.directive';
import { TitleDirective } from './directives/title.directive';
import { NodeHelperService } from './node-helper.service';
import { OptionsHelperService } from './options-helper.service';
import { KeysPipe } from './pipes/keys.pipe';
import { PermissionNamePipe } from './pipes/permission-name.pipe';
import { UrlPipe } from './pipes/url.pipe';
import { Toast } from './toast';
import {ImageConfigDirective} from './directives/image-config.directive';
import {ErrorProcessingService} from './error.processing';
import {ToastMessageComponent} from './components/toast-message/toast-message.component';
import {RenderHelperService} from './render-helper.service';
import {DragDropModule} from '@angular/cdk/drag-drop';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {NodeEntriesDragDirective} from './directives/node-entries-drag';
import {MatTableModule} from '@angular/material/table';
import {MatPaginatorModule} from '@angular/material/paginator';
import {MatSortModule} from '@angular/material/sort';
import {OverlayModule} from '@angular/cdk/overlay';
import {DragCursorDirective} from './directives/drag-cursor.directive';
import { SharedModule } from '../shared/shared.module';
import { ListItemsModule } from '../features/list-items/list-items.module';

@NgModule({
    declarations: [
        CollectionChooserComponent,
        ListTableComponent,
        NodeEntriesDragDirective,
        UserTileComponent,
        CustomNodeListWrapperComponent,
        VideoControlsComponent,
        InputPasswordComponent,
        ToastMessageComponent,
        KeysPipe,
        PermissionNamePipe,
        UrlPipe,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
        ImageConfigDirective,
        ListOptionItemComponent,
        DistinctClickDirective,
        DurationPipe,
        TitleDirective,
        DragCursorDirective,
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
        ListItemsModule,
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
        CustomNodeListWrapperComponent,
        ListOptionItemComponent,
        InputPasswordComponent,
        VideoControlsComponent,
        ImageConfigDirective,
        UserTileComponent,
        CollectionChooserComponent,
        KeysPipe,
        PermissionNamePipe,
        UrlPipe,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
        TitleDirective,
        DragCursorDirective,
    ],
})
export class CoreUiModule {}
