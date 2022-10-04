import { TemporaryStorageService } from './core-module/core.module';
import { SessionStorageService } from './core-module/core.module';
import { UIService } from './core-module/core.module';
import { CordovaService } from './common/services/cordova.service';
import { ConfigurationService } from './core-module/core.module';
import { FrameEventsService } from './core-module/core.module';
import { PermissionNamePipe } from './core-ui-module/pipes/permission-name.pipe';

export const PROVIDERS: any = [
    FrameEventsService,
    TemporaryStorageService,
    PermissionNamePipe,
    SessionStorageService,
    ConfigurationService,
    UIService,
    CordovaService,
];
