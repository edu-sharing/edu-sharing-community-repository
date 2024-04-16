import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ErrorPageComponent } from './error-page.component';
import { UIConstants } from '../../core-module/core.module';

const routes: Routes = [
    { path: UIConstants.ROUTER_PREFIX + 'messages/:message', component: ErrorPageComponent },
    { path: UIConstants.ROUTER_PREFIX + 'messages/:message/:text', component: ErrorPageComponent },
    { path: UIConstants.ROUTER_PREFIX + 'error/:message', component: ErrorPageComponent },
    { path: UIConstants.ROUTER_PREFIX + 'error/:message/:text', component: ErrorPageComponent },
    { path: '**', component: ErrorPageComponent, data: { message: 404 } },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class ErrorPageRoutingModule {}
