import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LtiPageComponent } from './lti.component';

const routes: Routes = [
    {
        path: '',
        component: LtiPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class LtiPageRoutingModule {}
