import { Injectable, InjectionToken, Injector, Type, inject } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { environment } from '../../environments/environment';

export type WebComponent = {
    name: string;
    component: Type<any>;
};
export const EDU_SHARING_WEB_COMPONENTS = new InjectionToken<WebComponent[]>(
    'EDU_SHARING_WEB_COMPONENTS',
);

/**
 * Service to register web components
 */
@Injectable({
    providedIn: 'root',
})
export class WebComponentService {
    private injector = inject(Injector);
    private components = inject(EDU_SHARING_WEB_COMPONENTS, { optional: true });

    constructor() {
        this.components?.forEach((c) => this.registerWebComponent(c.name, c.component));
    }

    /**
     * register your component as a web component
     * call this service from your own global service, provided in `extensionProviders`
     */
    registerWebComponent(name: string, component: Type<any>) {
        customElements.define(name, createCustomElement(component, { injector: this.injector }));
    }
}
