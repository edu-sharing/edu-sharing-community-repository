import { NgModule } from '@angular/core';
import { DashboardSwimlaneComponent } from '../../features/dashboard-swimlane/dashboard-swimlane.component';
import { ShortcutEntriesComponent } from '../../features/shortcut-entries/shortcut-entries.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { SharedModule } from '../../shared/shared.module';
import { LandingPageComponent } from './landing-page.component';
import { LandingPageRoutingModule } from './landing-page-routing.module';

@NgModule({
    declarations: [LandingPageComponent],
    imports: [
        DashboardSwimlaneComponent,
        FooterComponent,
        LandingPageRoutingModule,
        SharedModule,
        ShortcutEntriesComponent,
    ],
})
export class LandingPageModule {}
