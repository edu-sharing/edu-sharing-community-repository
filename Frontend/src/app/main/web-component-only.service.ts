import { ApplicationRef, createComponent, EnvironmentInjector, Injectable } from '@angular/core';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { CustomGlobalExtensionsComponent } from 'edu-sharing-extension-dependencies/custom-global-component/custom-global-extensions.component';

/**
 * Service that is ONLY injected when the application is running in a web component context
 */
@Injectable()
export class WebComponentOnlyService {
    constructor(
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
        console.log('enableCustomGlobalComponents', this.environmentInjector, this.appRef);
        const componentRef = createComponent(CustomGlobalExtensionsComponent, {
            environmentInjector: this.environmentInjector,
        });
        console.log('componentRef', componentRef);
        this.appRef.attachView(componentRef.hostView);
        console.log(
            'componentRef',
            componentRef,
            componentRef.location,
            componentRef.location?.nativeElement,
        );
        document.body.appendChild(componentRef.location.nativeElement);
    }
}
