import { NgModule } from '@angular/core';
import { NodeIconPipe } from '../pipes/node-icon.pipe';
import { VCardNamePipe } from '../pipes/vcard-name.pipe';
import { NodeImagePipe } from '../pipes/node-image.pipe';
import { IconDirective } from '../directives/icon.directive';
import { FormatSizePipe } from '../pipes/file-size.pipe';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { NodeImageSizePipe } from '../pipes/node-image-size.pipe';
import { NodePersonNamePipe } from '../pipes/node-person-name.pipe';
import { FormatDatePipe } from '../pipes/format-date.pipe';
import { SortDropdownComponent } from '../sort-dropdown/sort-dropdown.component';
import { CheckTextOverflowDirective } from '../directives/check-text-overflow.directive';
import { MatMenuModule } from '@angular/material/menu';
import { NodeTitlePipe } from '../pipes/node-title.pipe';
import { SpinnerComponent } from '../spinner/spinner.component';
import { NodeUrlComponent } from '../node-url/node-url.component';
import { DropdownComponent } from '../dropdown/dropdown.component';
import { RouterModule } from '@angular/router';
import { MatRippleModule } from '@angular/material/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OptionTooltipPipe } from '../pipes/option-tooltip.pipe';
import { ReplaceCharsPipe } from '../pipes/replace-chars.pipe';
import { ActionbarComponent } from '../actionbar/actionbar.component';

@NgModule({
    declarations: [
        IconDirective,
        CheckTextOverflowDirective,
        NodeIconPipe,
        NodeImagePipe,
        VCardNamePipe,
        FormatSizePipe,
        NodeImageSizePipe,
        NodePersonNamePipe,
        NodeTitlePipe,
        FormatDatePipe,
        ReplaceCharsPipe,
        SortDropdownComponent,
        SpinnerComponent,
        NodeUrlComponent,
        DropdownComponent,
        OptionTooltipPipe,
        ActionbarComponent,
    ],
    imports: [
        CommonModule,
        MatMenuModule,
        MatTooltipModule,
        MatRippleModule,
        TranslateModule,
        RouterModule,
    ],
    exports: [
        IconDirective,
        CheckTextOverflowDirective,
        NodeIconPipe,
        NodeImagePipe,
        VCardNamePipe,
        SortDropdownComponent,
        FormatSizePipe,
        NodeImageSizePipe,
        NodePersonNamePipe,
        NodeTitlePipe,
        FormatDatePipe,
        SpinnerComponent,
        NodeUrlComponent,
        DropdownComponent,
        ActionbarComponent,
    ],
})
export class EduSharingUiCommonModule {}
