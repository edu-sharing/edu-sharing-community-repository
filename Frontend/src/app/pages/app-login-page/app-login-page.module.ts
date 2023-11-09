import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { AppLoginPageRoutingModule } from './app-login-page-routing.module';
import { AppLoginPageComponent } from './app-login-page.component';

@NgModule({
    declarations: [AppLoginPageComponent],
    imports: [SharedModule, AppLoginPageRoutingModule],
})
export class AppLoginPageModule {}
