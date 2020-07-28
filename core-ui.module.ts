import { A11yModule } from '@angular/cdk/a11y';
import { HttpClient } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRippleModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { ToastyModule } from 'ngx-toasty';
import { RestLocatorService } from '../core-module/rest/services/rest-locator.service';
import { BreadcrumbsComponent } from './components/breadcrumbs/breadcrumbs.component';
import { CardComponent } from './components/card/card.component';
import { CollectionChooserComponent } from './components/collection-chooser/collection-chooser.component';
import { DropdownComponent } from './components/dropdown/dropdown.component';
import { GlobalProgressComponent } from './components/global-progress/global-progress.component';
import { IconComponent } from './components/icon/icon.component';
import { InfoMessageComponent } from './components/info-message/info-message.component';
import { InputPasswordComponent } from './components/input-password/input-password.component';
import { LinkComponent } from './components/link/link.component';
import { ListOptionItemComponent } from './components/list-option-item/list-option-item.component';
import { ListTableComponent } from './components/list-table/list-table.component';
import { SortDropdownComponent } from './components/sort-dropdown/sort-dropdown.component';
import { SpinnerSmallComponent } from './components/spinner-small/spinner-small.component';
import { SpinnerComponent } from './components/spinner/spinner.component';
import { UserAvatarComponent } from './components/user-avatar/user-avatar.component';
import { UserTileComponent } from './components/user-tile/user-tile.component';
import { VideoControlsComponent } from './components/video-controls/video-controls.component';
import { DistinctClickDirective } from './directives/distinct-click.directive';
import { NodesDragSourceDirective } from './directives/drag-nodes/nodes-drag-source.directive';
import { NodesDropTargetDirective } from './directives/drag-nodes/nodes-drop-target.directive';
import { InfiniteScrollDirective } from './directives/infinite-scroll.directive';
import { AuthorityAffiliationPipe } from './pipes/authority-affiliation.pipe';
import { AuthorityColorPipe } from './pipes/authority-color.pipe';
import { AuthorityNamePipe } from './pipes/authority-name.pipe';
import { NodeDatePipe } from './pipes/date.pipe';
import { FormatSizePipe } from './pipes/file-size.pipe';
import { KeysPipe } from './pipes/keys.pipe';
import { PermissionNamePipe } from './pipes/permission-name.pipe';
import { ReplaceCharsPipe } from './pipes/replace-chars.pipe';
import { SafeHtmlPipe } from './pipes/safe-html.pipe';
import { UrlPipe } from './pipes/url.pipe';
import { Toast } from './toast';
import { TranslationLoader } from './translation-loader';
import { Ng5SliderModule } from 'ng5-slider';
import {RouterLink, RouterModule} from "@angular/router";
import {NodeUrlComponent} from "./components/node-url/node-url.component";
import { DurationPipe } from './components/video-controls/duration.pipe';

@NgModule({
    declarations: [
        CollectionChooserComponent,
        ListTableComponent,
        DropdownComponent,
        SortDropdownComponent,
        IconComponent,
        CardComponent,
        UserAvatarComponent,
        UserTileComponent,
        LinkComponent,
        SpinnerComponent,
        BreadcrumbsComponent,
        SpinnerSmallComponent,
        GlobalProgressComponent,
        VideoControlsComponent,
        InfoMessageComponent,
        InputPasswordComponent,
        InfiniteScrollDirective,
        AuthorityNamePipe,
        AuthorityColorPipe,
        NodeDatePipe,
        NodeUrlComponent,
        FormatSizePipe,
        KeysPipe,
        ReplaceCharsPipe,
        PermissionNamePipe,
        UrlPipe,
        AuthorityAffiliationPipe,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
        SafeHtmlPipe,
        ListOptionItemComponent,
        DistinctClickDirective,
        DurationPipe,
    ],
    imports: [
        A11yModule,
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        MatCardModule,
        MatButtonModule,
        MatTabsModule,
        MatRadioModule,
        MatMenuModule,
        MatRippleModule,
        MatProgressBarModule,
        MatInputModule,
        MatCheckboxModule,
        MatProgressSpinnerModule,
        MatTooltipModule,
        Ng5SliderModule,
        RouterModule,
        ToastyModule.forRoot(),
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: TranslationLoader.create,
                deps: [HttpClient, RestLocatorService],
            },
        }),
    ],
    providers: [Toast],
    exports: [
        TranslateModule,
        ListTableComponent,
        SpinnerComponent,
        BreadcrumbsComponent,
        SpinnerSmallComponent,
        ListOptionItemComponent,
        InputPasswordComponent,
        GlobalProgressComponent,
        VideoControlsComponent,
        IconComponent,
        CardComponent,
        UserAvatarComponent,
        UserTileComponent,
        LinkComponent,
        CollectionChooserComponent,
        DropdownComponent,
        SortDropdownComponent,
        InfoMessageComponent,
        InfiniteScrollDirective,
        ToastyModule,
        AuthorityNamePipe,
        AuthorityColorPipe,
        NodeDatePipe,
        NodeUrlComponent,
        FormatSizePipe,
        KeysPipe,
        ReplaceCharsPipe,
        PermissionNamePipe,
        UrlPipe,
        AuthorityAffiliationPipe,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
    ],
})
export class CoreUiModule {}
