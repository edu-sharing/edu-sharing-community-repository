import {WorkspaceMainComponent} from "./workspace.component";
import {Routes} from "@angular/router";
import {WorkspaceRoutingComponent} from "./workspace-routing.component";


export const ROUTES_WORKSPACE=[
  { path: 'workspace/:mode', component: WorkspaceMainComponent},
  /*{ path: 'workspace', redirectTo: 'workspace/files',pathMatch: 'full'}  // ??? */
];
