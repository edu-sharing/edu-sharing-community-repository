import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { ApplyToLmsPageRoutingModule } from './apply-to-lms-page-routing.module';
import { ApplyToLmsPageComponent } from './apply-to-lms-page.component';

@NgModule({
    declarations: [ApplyToLmsPageComponent],
    imports: [SharedModule, ApplyToLmsPageRoutingModule],
})
export class ApplyToLmsPageModule {}
