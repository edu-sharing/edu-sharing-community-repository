import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { WorkspacePageComponent } from './workspace-page.component';

const routes: Routes = [
    {
        path: '',
        component: WorkspacePageComponent,
    },
    {
        path: ':mode',
        component: WorkspacePageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class WorkspacePageRoutingModule {}
