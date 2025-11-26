import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Oauth2consentPageComponent } from './oauth2consent-page.component';
import { Oauth2consentPageRoutingModule } from './oauth2consent-page.routing.module';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
    declarations: [Oauth2consentPageComponent],
    imports: [SharedModule, Oauth2consentPageRoutingModule],
})
export class Oauth2consentPageModule {}
