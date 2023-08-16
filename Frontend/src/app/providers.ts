import {
    ConfigurationService,
    FrameEventsService,
    SessionStorageService,
    UIService,
} from './core-module/core.module';
import { CordovaService } from './common/services/cordova.service';

export const PROVIDERS: any = [
    FrameEventsService,
    SessionStorageService,
    ConfigurationService,
    UIService,
    CordovaService,
];
