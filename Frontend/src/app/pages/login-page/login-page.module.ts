import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { LoginPageRoutingModule } from './login-page-routing.module';
import { LoginPageComponent } from './login-page.component';
import { UrlPipe } from './url.pipe';

@NgModule({
    declarations: [LoginPageComponent, UrlPipe],
    imports: [SharedModule, LoginPageRoutingModule],
})
export class LoginPageModule {}
