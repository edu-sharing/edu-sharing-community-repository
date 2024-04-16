import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { UploadPageRoutingModule } from './upload-page-routing.module';
import { UploadPageComponent } from './upload-page.component';

@NgModule({
    declarations: [UploadPageComponent],
    imports: [SharedModule, UploadPageRoutingModule],
})
export class UploadPageModule {}
