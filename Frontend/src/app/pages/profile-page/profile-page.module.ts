import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { ProfilePageRoutingModule } from './profile-page-routing.module';
import { ProfilePageComponent } from './profile-page.component';

@NgModule({
    declarations: [ProfilePageComponent],
    imports: [SharedModule, ProfilePageRoutingModule],
})
export class ProfilePageModule {}
