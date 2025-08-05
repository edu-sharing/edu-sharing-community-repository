import { Location } from '@angular/common';
import { HttpRequest } from '@angular/common/http';
import { Injectable, Injector, Type } from '@angular/core';
import { type ApiErrorResponse } from 'ngx-edu-sharing-api';
import { RestConstants } from '../core-module/core.module';
import { DialogsService } from '../features/dialogs/dialogs.service';
import { CordovaService } from '../services/cordova.service';
import { Toast } from '../services/toast';
import { createCustomElement } from '@angular/elements';
import { ActionbarComponent } from 'ngx-edu-sharing-ui';

@Injectable({
    providedIn: 'root',
})
export class WebComponentService {
    constructor(private injector: Injector) {}

    /**
     * register your component as a web component
     * call this service from your own global service, provided in `extensionProviders`
     */
    registerWebComponent(name: string, component: Type<any>) {
        customElements.define(name, createCustomElement(component, { injector: this.injector }));
    }
}
