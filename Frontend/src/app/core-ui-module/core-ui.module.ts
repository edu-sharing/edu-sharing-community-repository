import { A11yModule } from '@angular/cdk/a11y';
import { HttpClient } from '@angular/common/http';
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
import { MissingTranslationHandler, TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { Ng5SliderModule } from 'ng5-slider';
import { ToastyModule } from 'ngx-toasty';
import { RestLocatorService } from '../core-module/rest/services/rest-locator.service';
import { BreadcrumbsComponent } from './components/breadcrumbs/breadcrumbs.component';
import { CardComponent } from './components/card/card.component';
import { CollectionChooserComponent } from './components/collection-chooser/collection-chooser.component';
import { CustomNodeListWrapperComponent } from './components/custom-node-list-wrapper/custom-node-list-wrapper.component';
import { DropdownComponent } from './components/dropdown/dropdown.component';
import { GlobalProgressComponent } from './components/global-progress/global-progress.component';
import { IconDirective } from './components/icon/icon.directive';
import { InfoMessageComponent } from './components/info-message/info-message.component';
import { InputPasswordComponent } from './components/input-password/input-password.component';
import { LinkComponent } from './components/link/link.component';
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
import { SpinnerSmallComponent } from './components/spinner-small/spinner-small.component';
import { SpinnerComponent } from './components/spinner/spinner.component';
import { UserAvatarComponent } from './components/user-avatar/user-avatar.component';
import { UserTileComponent } from './components/user-tile/user-tile.component';
import { DurationPipe } from './components/video-controls/duration.pipe';
import { VideoControlsComponent } from './components/video-controls/video-controls.component';
import { DistinctClickDirective } from './directives/distinct-click.directive';
import { NodesDragSourceDirective } from './directives/drag-nodes/nodes-drag-source.directive';
import { NodesDropTargetDirective } from './directives/drag-nodes/nodes-drop-target.directive';
import { ElementRefDirective } from './directives/element-ref.directive';
import { InfiniteScrollDirective } from './directives/infinite-scroll.directive';
import { TitleDirective } from './directives/title.directive';
import { NodeHelperService } from './node-helper.service';
import { OptionsHelperService } from './options-helper.service';
import { AuthorityAffiliationPipe } from './pipes/authority-affiliation.pipe';
import { AuthorityColorPipe } from './pipes/authority-color.pipe';
import { AuthorityNamePipe } from './pipes/authority-name.pipe';
import { BitwisePipe } from './pipes/bitwise.pipe';
import { NodeDatePipe } from './pipes/date.pipe';
import { FormatSizePipe } from './pipes/file-size.pipe';
import { KeysPipe } from './pipes/keys.pipe';
import { NodeImageSizePipe } from './pipes/node-image-size.pipe';
import { NodePersonNamePipe } from './pipes/node-person-name.pipe';
import { NodeSourcePipe } from './pipes/node-source.pipe';
import { PermissionNamePipe } from './pipes/permission-name.pipe';
import { ReplaceCharsPipe } from './pipes/replace-chars.pipe';
import { SafeHtmlPipe } from './pipes/safe-html.pipe';
import { UrlPipe } from './pipes/url.pipe';
import { Toast } from './toast';
import { FallbackTranslationHandler } from './translation';
import { TranslationLoader } from './translation-loader';
import {VCardNamePipe} from './pipes/vcard-name.pipe';
import {ImageConfigDirective} from './directives/image-config.directive';
import {NativeElementInjectorDirective} from './directives/native-element-injector.directive';
import {ToastMessageComponent} from './components/toast-message/toast-message.component';
import {OptionTooltipPipe} from './pipes/option-tooltip.pipe';

@NgModule({
    declarations: [
        CollectionChooserComponent,
        ListTableComponent,
        ListBaseComponent,
        ListCollectionInfoComponent,
        ListNodeLicenseComponent,
        ListNodeReplicationSourceComponent,
        ListNodeWorkflowComponent,
        ListTextComponent,
        ListCountsComponent,
        DropdownComponent,
        SortDropdownComponent,
        IconDirective,
        CardComponent,
        UserAvatarComponent,
        UserTileComponent,
        LinkComponent,
        SpinnerComponent,
        CustomNodeListWrapperComponent,
        BreadcrumbsComponent,
        SpinnerSmallComponent,
        GlobalProgressComponent,
        VideoControlsComponent,
        NativeElementInjectorDirective,
        InfoMessageComponent,
        InputPasswordComponent,
        InfiniteScrollDirective,
        AuthorityNamePipe,
        VCardNamePipe,
        NodePersonNamePipe,
        AuthorityColorPipe,
        NodeDatePipe,
        NodeUrlComponent,
        ToastMessageComponent,
        FormatSizePipe,
        KeysPipe,
        ReplaceCharsPipe,
        PermissionNamePipe,
        NodeImageSizePipe,
        UrlPipe,
        AuthorityAffiliationPipe,
        OptionTooltipPipe,
        NodeSourcePipe,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
        ImageConfigDirective,
        SafeHtmlPipe,
        ListOptionItemComponent,
        DistinctClickDirective,
        DurationPipe,
        BitwisePipe,
        ElementRefDirective,
        TitleDirective,
    ],
    imports: [
        A11yModule,
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
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
        Ng5SliderModule,
        RouterModule,
        ToastyModule.forRoot(),
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: TranslationLoader.create,
                deps: [HttpClient, RestLocatorService],
            },
            missingTranslationHandler: {
                provide: MissingTranslationHandler,
                useClass: FallbackTranslationHandler,
            },
        }),
    ],
    providers: [Toast, NodeHelperService, OptionsHelperService],
    exports: [
        TranslateModule,
        ListTableComponent,
        SpinnerComponent,
        CustomNodeListWrapperComponent,
        BreadcrumbsComponent,
        SpinnerSmallComponent,
        ListOptionItemComponent,
        InputPasswordComponent,
        GlobalProgressComponent,
        VideoControlsComponent,
        IconDirective,
        ImageConfigDirective,
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
        OptionTooltipPipe,
        NativeElementInjectorDirective,
        VCardNamePipe,
        AuthorityColorPipe,
        NodeDatePipe,
        NodeUrlComponent,
        FormatSizePipe,
        KeysPipe,
        ReplaceCharsPipe,
        PermissionNamePipe,
        UrlPipe,
        AuthorityAffiliationPipe,
        NodeSourcePipe,
        NodesDragSourceDirective,
        NodesDropTargetDirective,
        ListCollectionInfoComponent,
        ListBaseComponent,
        ListTextComponent,
        TitleDirective,
        SafeHtmlPipe,
        ElementRefDirective,
    ],
})
export class CoreUiModule {}
