import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { ErrorPageRoutingModule } from './error-page-routing.module';
import { ErrorPageComponent } from './error-page.component';

@NgModule({
    declarations: [ErrorPageComponent],
    imports: [SharedModule, ErrorPageRoutingModule],
})
export class ErrorPageModule {}
