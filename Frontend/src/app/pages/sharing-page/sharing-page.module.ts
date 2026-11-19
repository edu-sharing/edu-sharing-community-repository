import { NgModule } from '@angular/core';
import { RenderingServiceLibModule } from 'ngx-rendering-service-lib';
import { SharedModule } from '../../shared/shared.module';
import { SharingPageRoutingModule } from './sharing-page-routing.module';
import { SharingPageComponent } from './sharing-page.component';

@NgModule({
    declarations: [SharingPageComponent],
    imports: [SharedModule, SharingPageRoutingModule, RenderingServiceLibModule],
})
export class SharingPageModule {}
