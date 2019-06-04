import {Toast} from "./common/ui/toast";
import {TemporaryStorageService} from "./core-module/core.module";
import {SessionStorageService} from "./core-module/core.module";
import {UIService} from "./core-module/core.module";
import {CordovaService} from "./common/services/cordova.service";
import {ConfigurationService} from "./core-module/core.module";
import {FrameEventsService} from "./core-module/core.module";
import {ActionbarHelperService} from "./common/services/actionbar-helper";
import {PermissionNamePipe} from "./common/ui/permission-name.pipe";


export const PROVIDERS:any=[
  FrameEventsService,
  Toast,
  TemporaryStorageService,
  ActionbarHelperService,
  PermissionNamePipe,
  SessionStorageService,
  ConfigurationService,
  UIService,
  CordovaService
];
