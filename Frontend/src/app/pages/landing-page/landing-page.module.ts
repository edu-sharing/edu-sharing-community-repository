import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { LandingPageComponent } from './landing-page.component';
import { LandingPageRoutingModule } from './landing-page-routing.module';
import { ShortcutEntriesComponent } from 'ngx-edu-sharing-ui';

@NgModule({
    declarations: [LandingPageComponent],
    imports: [SharedModule, ShortcutEntriesComponent, LandingPageRoutingModule],
})
export class LandingPageModule {}
