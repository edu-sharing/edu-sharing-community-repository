import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RegisterPageComponent } from './register-page.component';

const routes: Routes = [
    {
        path: '',
        component: RegisterPageComponent,
    },
    {
        path: ':status',
        component: RegisterPageComponent,
    },
    {
        path: ':status/:key',
        component: RegisterPageComponent,
    },
    {
        path: ':status/:key/:email',
        component: RegisterPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class RegisterPageRoutingModule {}
