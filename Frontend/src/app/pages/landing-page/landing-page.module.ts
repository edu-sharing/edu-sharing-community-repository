import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { LandingPageComponent } from './landing-page.component';
import { LandingPageRoutingModule } from './landing-page-routing.module';
import { ShortcutEntriesComponent } from 'ngx-edu-sharing-ui';
import { DashboardSwimlaneComponent } from '../../features/dashboard-swimlane/dashboard-swimlane.component';

@NgModule({
    declarations: [LandingPageComponent],
    imports: [
        SharedModule,
        ShortcutEntriesComponent,
        DashboardSwimlaneComponent,
        LandingPageRoutingModule,
    ],
})
export class LandingPageModule {}
