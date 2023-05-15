import { NgModule } from '@angular/core';
import { CoreModule } from '../app/core-module/core.module';
import { CoreUiModule } from '../app/core-ui-module/core-ui.module';
import { OptionsHelperService } from '../app/core-ui-module/options-helper.service';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatRadioModule } from '@angular/material/radio';
import { MatMenuModule } from '@angular/material/menu';
import { MatRippleModule } from '@angular/material/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@NgModule({
    imports: [MatProgressSpinnerModule],
})
export class StorybookModule {}
