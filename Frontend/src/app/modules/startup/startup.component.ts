import { Component } from '@angular/core';
import { CordovaService } from '../../common/services/cordova.service';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { Router } from '@angular/router';

@Component({
    selector: 'es-startup',
    template: '',
})
export class StartupComponent {
    constructor(private cordova: CordovaService, private router: Router) {
        if (this.cordova.isRunningCordova()) {
            // wait until cordova device init is ready
            this.cordova.subscribeServiceReady().subscribe(() => {
                // per default go to app
                this.router.navigate([UIConstants.ROUTER_PREFIX, 'app'], { replaceUrl: true });
            });
        } else {
            this.router.navigate([UIConstants.ROUTER_PREFIX, 'login'], { replaceUrl: true });
        }
    }
}
