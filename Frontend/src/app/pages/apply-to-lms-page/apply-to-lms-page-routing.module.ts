import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ApplyToLmsPageComponent } from './apply-to-lms-page.component';

const routes: Routes = [
    {
        path: ':repo/:node',
        component: ApplyToLmsPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class ApplyToLmsPageRoutingModule {}
