// Overwrite this file in extensions to define additional routes.

import { Routes } from '@angular/router';
import { UIConstants } from '../core-module/core.module';
import { EditorialDashboardComponent } from './editorial-dashboard/editorial-dashboard.component';
import { EditorialDeskComponent } from './editorial-desk/editorial-desk.component';
import { BoerdSidebarComponent } from './boerd-sidebar/boerd-sidebar.component';

export const extensionRoutes: Routes = [
    { path: UIConstants.ROUTER_PREFIX + 'editorial-desk', component: EditorialDeskComponent },
    {
        path: UIConstants.ROUTER_PREFIX + 'editorial-dashboard',
        component: EditorialDashboardComponent,
    },
    {
        path: UIConstants.ROUTER_PREFIX + 'boerd-sidebar',
        component: BoerdSidebarComponent,
    },
];
