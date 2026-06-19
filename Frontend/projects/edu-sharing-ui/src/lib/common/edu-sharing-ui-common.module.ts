import { CommonModule } from '@angular/common';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatRippleModule } from '@angular/material/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ActionbarComponent } from '../actionbar/actionbar.component';
import { BorderBoxObserverDirective } from '../directives/border-box-observer.directive';
import { CheckTextOverflowDirective } from '../directives/check-text-overflow.directive';
import { FocusStateDirective } from '../directives/focus-state.directive';
import { IconDirective } from '../directives/icon.directive';
import { InfiniteScrollDirective } from '../directives/infinite-scroll.directive';
import { DropdownComponent } from '../dropdown/dropdown.component';
import { NodeUrlComponent } from '../node-url/node-url.component';
import { FormatSizePipe } from '../pipes/file-size.pipe';
import { FormatDatePipe } from '../pipes/format-date.pipe';
import { NodeIconPipe } from '../pipes/node-icon.pipe';
import { NodeImageSizePipe } from '../pipes/node-image-size.pipe';
import { NodeImagePipe } from '../pipes/node-image.pipe';
import { NodePersonNamePipe } from '../pipes/node-person-name.pipe';
import { NodeTitlePipe } from '../pipes/node-title.pipe';
import { OptionTooltipPipe } from '../pipes/option-tooltip.pipe';
import { ReplaceCharsPipe } from '../pipes/replace-chars.pipe';
import { VCardNamePipe } from '../pipes/vcard-name.pipe';
import { SortDropdownComponent } from '../sort-dropdown/sort-dropdown.component';
import { SpinnerComponent } from '../spinner/spinner.component';
import { PropertySlugPipe } from '../pipes/property-slug.pipe';
import { NodeLicensePipe } from '../pipes/node-license.pipe';
import { MdsWidgetComponent } from '../mds-viewer/widget/mds-widget.component';
import { MdsDurationPipe } from '../pipes/mds-duration.pipe';
import { MdsViewerComponent } from '../mds-viewer/mds-viewer.component';
import { SpinnerSmallComponent } from '../spinner-small/spinner-small.component';
import { NodeUrlPipe } from '../pipes/node-url.pipe';
import { ToolpermissionPipe } from '../pipes/toolpermission.pipe';
import { AuthorityNamePipe } from '../pipes/authority-name.pipe';
import { HtmlTextPipe } from '../pipes/html-text.pipe';
import { AssignmentPipe } from '../pipes/assignment.pipe';
import { AuthorityColorPipe } from '../pipes/authority-color.pipe';
import { AssetsPathPipe } from '../pipes/assets-path.pipe';
import { InfoMessageComponent } from '../info-message/info-message.component';
import { MatBottomSheetModule } from '@angular/material/bottom-sheet';
import { DropdownBottomSheetComponent } from '../dropdown/dropdown-bottom-sheet/dropdown-bottom-sheet.component';

@NgModule({
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    declarations: [
        MdsViewerComponent,
        MdsWidgetComponent,
        ActionbarComponent,
        BorderBoxObserverDirective,
        CheckTextOverflowDirective,
        DropdownComponent,
        DropdownBottomSheetComponent,
        FocusStateDirective,
        FormatDatePipe,
        FormatSizePipe,
        MdsDurationPipe,
        InfiniteScrollDirective,
        PropertySlugPipe,
        NodeIconPipe,
        NodeUrlPipe,
        AuthorityNamePipe,
        AuthorityColorPipe,
        AssetsPathPipe,
        NodeImagePipe,
        NodeImageSizePipe,
        NodeLicensePipe,
        NodePersonNamePipe,
        NodeTitlePipe,
        AssignmentPipe,
        NodeUrlComponent,
        OptionTooltipPipe,
        ReplaceCharsPipe,
        SortDropdownComponent,
        VCardNamePipe,
    ],
    imports: [
        CommonModule,
        MatMenuModule,
        SpinnerComponent,
        IconDirective,
        SpinnerSmallComponent,
        InfoMessageComponent,
        ToolpermissionPipe,
        MatButtonModule,
        MatBottomSheetModule,
        HtmlTextPipe,
        MatTooltipModule,
        MatRippleModule,
        TranslateModule,
        RouterModule,
    ],
    exports: [
        MdsViewerComponent,
        MdsWidgetComponent,
        ActionbarComponent,
        BorderBoxObserverDirective,
        CheckTextOverflowDirective,
        DropdownComponent,
        FocusStateDirective,
        FormatDatePipe,
        FormatSizePipe,
        MdsDurationPipe,
        OptionTooltipPipe,
        IconDirective,
        HtmlTextPipe,
        InfiniteScrollDirective,
        InfoMessageComponent,
        PropertySlugPipe,
        NodeIconPipe,
        NodeUrlPipe,
        AuthorityNamePipe,
        AuthorityColorPipe,
        AssetsPathPipe,
        NodeImagePipe,
        NodeLicensePipe,
        NodeImageSizePipe,
        NodePersonNamePipe,
        NodeTitlePipe,
        AssignmentPipe,
        NodeUrlComponent,
        ToolpermissionPipe,
        SortDropdownComponent,
        SpinnerComponent,
        SpinnerSmallComponent,
        VCardNamePipe,
    ],
})
export class EduSharingUiCommonModule {}
