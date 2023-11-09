import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { AppSharePageRoutingModule } from './app-share-page-routing.module';
import { AppSharePageComponent } from './app-share-page.component';

@NgModule({
    declarations: [AppSharePageComponent],
    imports: [SharedModule, AppSharePageRoutingModule],
})
export class AppSharePageModule {}
