import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AppLoginPageComponent } from './app-login-page.component';

const routes: Routes = [
    {
        path: '',
        component: AppLoginPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class AppLoginPageRoutingModule {}
