import { Injectable } from '@angular/core';
import { AppService as AppServiceAbstract } from 'ngx-edu-sharing-ui';
import { CordovaService } from './cordova.service';

declare var cordova: any;

@Injectable({ providedIn: 'root' })
export class AppService extends AppServiceAbstract {
    constructor(private cordovaService: CordovaService) {
        super();
    }
    isRunningApp(): boolean {
        return this.cordovaService.isRunningApp();
    }
    getLanguage(): Promise<string> {
        return this.cordovaService.getLanguage();
    }
}
