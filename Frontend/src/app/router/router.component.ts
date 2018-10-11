import { Component } from '@angular/core';
import {RestTestComponent} from "../common/rest/directives/rest-test/rest-test.component";
import {NodeRenderComponent} from "../common/ui/node-render/node-render.component";
import {Router, ActivatedRoute} from "@angular/router";
import { ApplyToLmsComponent} from "../common/ui/apply-to-lms/apply-to-lms.component";
import {SearchComponent} from "../modules/search/search.component";
import {WorkspaceMainComponent} from "../modules/workspace/workspace.component";
import {RecycleMainComponent} from "../modules/node-list/recycle/recycle.component";
import {TasksMainComponent} from "../modules/node-list/tasks/tasks.component";
import {CollectionNewComponent} from "../modules/collections/collection-new/collection-new.component";
import {CollectionsMainComponent} from "../modules/collections/collections.component";
import {LoginComponent} from "../modules/login/login.component";
import {LoginAppComponent} from "../modules/login-app/login-app.component";
import {PermissionsRoutingComponent} from "../modules/permissions/permissions-routing.component";
import {PermissionsMainComponent} from "../modules/permissions/permissions.component";
import {OerComponent} from "../modules/oer/oer.component";
import {MdsTestComponent} from "../common/test/mds-test/mds-test.component";
import {ConfigurationService} from "../common/services/configuration.service";
import {AdminComponent} from "../modules/admin/admin.component";
import {MessagesComponent} from "../modules/messages/messages.component";
import {UIConstants} from "../common/ui/ui-constants";
import {StreamComponent} from "../modules/stream/stream.component";
import {ProfilesComponent} from "../modules/profiles/profiles.component";
import {StartupComponent} from '../modules/startup/startup.component';
import {ShareAppComponent} from "../modules/share-app/share-app.component";
import {SharingComponent} from "../modules/sharing/sharing.component";
import {RegisterComponent} from '../modules/register/register.component';
import {ServicesComponent} from "../modules/services/services.components";



@Component({
  selector: 'router',
  templateUrl: 'router.component.html'
})
export class RouterComponent {
  /**
   * adds a prefix to all routes for compatibility with tomcat
   * @param route
   * @returns {any}
   */
  static transformRoute(route: any): any {
    let result: any = []; // we need a deep copy
    for (let r of route) {
      let a: any = {
        path: r.path,
        component: r.component,
        children: r.children
      };

      if (a.path)
        a.path = UIConstants.ROUTER_PREFIX + r.path;
      result.push(a);
    }
    return result;
  }
}
// RouterComponent.transformRoute
// this fails for aot because it can't call static functions
/*

 export var ROUTES_COMMON:any=[
 { path: '', component: NoRouteComponent },
 { path: 'rest-test',component: RestTestComponent},
 { path: 'render/:node', component: NodeRenderComponent},
 { path: 'render/:node/:version', component: NodeRenderComponent},
 { path: 'apply-to-lms/:node', component: ApplyToLmsComponent}
 ];
 .concat(ROUTES_SEARCH)
 .concat(ROUTES_WORKSPACE)
 .concat(ROUTES_RECYCLE)
 .concat(ROUTES_COLLECTIONS)
 .concat(ROUTES_LOGIN)
 .concat(ROUTES_PERMISSIONS)
 */

// Due to ahead of time, we need to create all routes manuall
export var ROUTES=[
  // global
    { path: '', component: StartupComponent },
    { path: UIConstants.ROUTER_PREFIX+'app', component: LoginAppComponent },
    { path: UIConstants.ROUTER_PREFIX+'app/share', component: ShareAppComponent },
    { path: UIConstants.ROUTER_PREFIX+'sharing', component: ShareAppComponent },
    { path: UIConstants.ROUTER_PREFIX+'test/mds',component: MdsTestComponent},
    { path: UIConstants.ROUTER_PREFIX+'test/rest',component: RestTestComponent},
    { path: UIConstants.ROUTER_PREFIX+'render/:node', component: NodeRenderComponent},
    { path: UIConstants.ROUTER_PREFIX+'render/:node/:version', component: NodeRenderComponent},
    { path: UIConstants.ROUTER_PREFIX+'apply-to-lms/:repo/:node', component: ApplyToLmsComponent},
  // search
    { path: UIConstants.ROUTER_PREFIX+'search', component: SearchComponent },
  // workspace
    { path: UIConstants.ROUTER_PREFIX+'workspace', component: WorkspaceMainComponent},
    { path: UIConstants.ROUTER_PREFIX+'workspace/:mode', component: WorkspaceMainComponent},
  // recycle/node component
    { path: UIConstants.ROUTER_PREFIX+'recycle', component: RecycleMainComponent},
    { path: UIConstants.ROUTER_PREFIX+'tasks', component: TasksMainComponent},
  // collections
    { path: UIConstants.ROUTER_PREFIX+'collections', component: CollectionsMainComponent},
    { path: UIConstants.ROUTER_PREFIX+'collections/collection/:mode/:id', component: CollectionNewComponent},
  // login
    { path: UIConstants.ROUTER_PREFIX+'login', component: LoginComponent },
  // register
    { path: UIConstants.ROUTER_PREFIX+'register', component: RegisterComponent },
    { path: UIConstants.ROUTER_PREFIX+'register/:status', component: RegisterComponent },
    { path: UIConstants.ROUTER_PREFIX+'register/:status/:key', component: RegisterComponent },
    { path: UIConstants.ROUTER_PREFIX+'register/:status/:key/:email', component: RegisterComponent },
  // admin
    { path: UIConstants.ROUTER_PREFIX+'admin', component: AdminComponent },
  // permissions
    { path: UIConstants.ROUTER_PREFIX+'permissions', component: PermissionsRoutingComponent,
      children:[
        { path: '', component: PermissionsMainComponent }
      ]

    },
    // oer
    { path: UIConstants.ROUTER_PREFIX+'oer', component: OerComponent },
    // stream
    { path: UIConstants.ROUTER_PREFIX+'stream', component: StreamComponent },
    // profiles
    { path: UIConstants.ROUTER_PREFIX+'profiles/:authority', component: ProfilesComponent },

    // messages
    { path: UIConstants.ROUTER_PREFIX+'messages/:message', component: MessagesComponent },

    // link-share
    { path: UIConstants.ROUTER_PREFIX+'sharing', component: SharingComponent },
    // services
    { path: UIConstants.ROUTER_PREFIX+'services', component: ServicesComponent },
  ]
;

