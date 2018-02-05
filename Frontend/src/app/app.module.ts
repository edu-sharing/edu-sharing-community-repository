import {NgModule} from '@angular/core';
import {DECLARATIONS} from "./declarations";
import {IMPORTS} from "./imports";
import {PROVIDERS} from "./providers";
import {RouterComponent} from "./router/router.component";
import {DECLARATIONS_RECYCLE} from "./modules/node-list/declarations";
import {DECLARATIONS_WORKSPACE} from "./modules/workspace/declarations";
import {DECLARATIONS_SEARCH} from "./modules/search/declarations";
import {PROVIDERS_SEARCH} from "./modules/search/providers";
import {DECLARATIONS_COLLECTIONS} from "./modules/collections/declarations";
import {DECLARATIONS_LOGIN} from "./modules/login/declarations";
import {DECLARATIONS_PERMISSIONS} from "./modules/permissions/declarations";
import {DECLARATIONS_OER} from "./modules/oer/declarations";
import {DECLARATIONS_ADMIN} from "./modules/admin/declarations";
import {DECLARATIONS_MANAGEMENT_DIALOGS} from "./modules/management-dialogs/declarations";
import {DECLARATIONS_MESSAGES} from "./modules/messages/declarations";
import {DECLARATIONS_UPLOAD} from "./modules/upload/declarations";
import {DECLARATIONS_STREAM} from "./modules/stream/declarations";
import {DECLARATIONS_PROFILES} from "./modules/profiles/declarations";



// http://blog.angular-university.io/angular2-ngmodule/
// -> Making modules more readable using the spread operator


@NgModule({
  declarations: [
    DECLARATIONS,
    DECLARATIONS_RECYCLE,
    DECLARATIONS_WORKSPACE,
    DECLARATIONS_SEARCH,
    DECLARATIONS_COLLECTIONS,
    DECLARATIONS_LOGIN,
    DECLARATIONS_PERMISSIONS,
    DECLARATIONS_OER,
    DECLARATIONS_STREAM,
    DECLARATIONS_MANAGEMENT_DIALOGS,
    DECLARATIONS_ADMIN,
    DECLARATIONS_UPLOAD,
    DECLARATIONS_PROFILES,
    DECLARATIONS_MESSAGES
  ],
  imports: [
    IMPORTS,
  ],
  providers: [
    PROVIDERS,
    PROVIDERS_SEARCH
  ],
  bootstrap: [RouterComponent]
})
export class AppModule { }
