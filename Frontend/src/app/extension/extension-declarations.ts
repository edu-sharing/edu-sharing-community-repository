import { Type } from '@angular/core';
import { EditorialDashboardComponent } from './editorial-dashboard/editorial-dashboard.component';
import { EditorialDeskComponent } from './editorial-desk/editorial-desk.component';
import { WorkflowActionCardComponent } from './workflow-action-card/workflow-action-card.component';
import { WorkflowPanelComponent } from './workflow-panel/workflow-panel.component';
import { WorkflowStatusComponent } from './workflow-status/workflow-status.component';
import { WrapObservablePipe } from './wrap-observable.pipe';
import { NodesRenderComponent } from './nodes-render/nodes-render.component';
import { BoerdSidebarComponent } from './boerd-sidebar/boerd-sidebar.component';

export const extensionDeclarations: Type<any>[] = [
    EditorialDeskComponent,
    EditorialDashboardComponent,
    WrapObservablePipe,
    NodesRenderComponent,
    WorkflowPanelComponent,
    WorkflowActionCardComponent,
    WorkflowStatusComponent,
    BoerdSidebarComponent,
];
