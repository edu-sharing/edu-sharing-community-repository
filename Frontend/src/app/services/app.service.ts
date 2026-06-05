import { Injectable, inject } from '@angular/core';
import { AppService as AppServiceAbstract } from 'ngx-edu-sharing-ui';
import { CordovaService } from './cordova.service';

declare var cordova: any;

@Injectable({ providedIn: 'root' })
export class AppService extends AppServiceAbstract {
    private cordovaService = inject(CordovaService);

    isRunningApp(): boolean {
        return this.cordovaService.isRunningApp();
    }
    getLanguage(): Promise<string> {
        return this.cordovaService.getLanguage();
    }
}
