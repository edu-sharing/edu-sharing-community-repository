import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { SharingPageRoutingModule } from './sharing-page-routing.module';
import { SharingPageComponent } from './sharing-page.component';

@NgModule({
    declarations: [SharingPageComponent],
    imports: [SharedModule, SharingPageRoutingModule],
})
export class SharingPageModule {}
