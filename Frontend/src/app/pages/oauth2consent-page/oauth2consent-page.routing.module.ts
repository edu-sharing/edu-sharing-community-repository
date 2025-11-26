import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { Oauth2consentPageComponent } from './oauth2consent-page.component';

const routes: Routes = [
    {
        path: '',
        component: Oauth2consentPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class Oauth2consentPageRoutingModule {}
