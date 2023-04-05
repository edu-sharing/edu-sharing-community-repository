import { ModuleWithProviders, NgModule } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { EduSharingUiConfigurationParams, getConfigProvider } from './edu-sharing-ui-configuration';
import { ListItemsModule } from './list-items/list-items.module';
import { NodeEntriesModule } from './node-entries/node-entries.module';
import { EduSharingUiCommonModule } from './common/edu-sharing-ui-common.module';
import { NodeImageSizePipe } from './pipes/node-image-size.pipe';
import { FormatDatePipe } from './pipes/format-date.pipe';
import { ListItemLabelPipe } from './node-entries/list-item-label.pipe';
import { SortDropdownComponent } from './sort-dropdown/sort-dropdown.component';
import { TranslationsModule } from './translations/translations.module';

@NgModule({
    declarations: [],
    imports: [
        CommonModule,
        TranslateModule,
        EduSharingUiCommonModule,
        ListItemsModule,
        NodeEntriesModule,
        TranslationsModule.forRoot(),
    ],
    exports: [
        CommonModule,
        MatTooltipModule,
        TranslateModule,
        EduSharingUiCommonModule,
        ListItemsModule,
        NodeEntriesModule,
        NodeImageSizePipe,
        FormatDatePipe,
        ListItemsModule,
        ListItemLabelPipe,
        SortDropdownComponent,
        TranslationsModule,
    ],
})
export class EduSharingUiModule {
    public static forRoot(
        config: EduSharingUiConfigurationParams,
    ): ModuleWithProviders<EduSharingUiModule> {
        return {
            ngModule: EduSharingUiModule,
            providers: [getConfigProvider(config)],
        };
    }
}
