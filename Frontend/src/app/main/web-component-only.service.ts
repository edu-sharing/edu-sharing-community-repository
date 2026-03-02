import {
    ApplicationRef,
    createComponent,
    EnvironmentInjector,
    Injectable,
    Injector,
    Type,
} from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { CustomGlobalExtensionsComponent } from 'edu-sharing-extension-dependencies/custom-global-component/custom-global-extensions.component';

/**
 * Service that is ONLY injected when the application is running in a web component context
 */
@Injectable()
export class WebComponentOnlyService {
    constructor(
        private injector: Injector,
        private translations: TranslationsService,
        private environmentInjector: EnvironmentInjector,
        private appRef: ApplicationRef,
    ) {
        this.translations.initialize().subscribe();
        this.enableCustomGlobalComponents();
    }

    /**
     * some projects might register custom templates or configs in the custom global component
     * so, we will instance it globally in web component context
     * @private
     */
    private enableCustomGlobalComponents() {
        const componentRef = createComponent(CustomGlobalExtensionsComponent, {
            environmentInjector: this.environmentInjector,
        });
        this.appRef.attachView(componentRef.hostView);
        document.body.appendChild(componentRef.location.nativeElement);
    }

    /**
     * register your component as a web component
     * call this service from your own global service, provided in `extensionProviders`
     */
    registerWebComponent(name: string, component: Type<any>) {
        customElements.define(name, createCustomElement(component, { injector: this.injector }));
    }
}
