import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { RegisterDoneComponent } from './register-done/register-done.component';
import { RegisterFormComponent } from './register-form/register-form.component';
import { RegisterPageRoutingModule } from './register-page-routing.module';
import { RegisterPageComponent } from './register-page.component';
import { RegisterRequestComponent } from './register-request/register-request.component';
import { RegisterResetPasswordComponent } from './register-reset-password/register-reset-password.component';

@NgModule({
    declarations: [
        RegisterPageComponent,
        RegisterFormComponent,
        RegisterDoneComponent,
        RegisterRequestComponent,
        RegisterResetPasswordComponent,
    ],
    imports: [SharedModule, RegisterPageRoutingModule],
})
export class RegisterPageModule {}
