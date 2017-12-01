

import {PermissionsRoutingComponent} from "./permissions-routing.component";
import {PermissionsMainComponent} from "./permissions.component";
export const ROUTES_PERMISSIONS=[
  { path: 'permissions', component: PermissionsRoutingComponent,
    children:[
      { path: '', component: PermissionsMainComponent }
    ]

  }
];
