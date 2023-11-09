import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { LtiPageRoutingModule } from './lti-page-routing.module';
import { LtiPageComponent } from './lti.component';

@NgModule({
    declarations: [LtiPageComponent],
    imports: [SharedModule, LtiPageRoutingModule],
})
export class LtiPageModule {}
