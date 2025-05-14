import { ApplicationRef, DoBootstrap, Injector, NgModule } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { AppComponent } from './app.component';
import { BrowserModule } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import {
    EduSharingUiModule,
    TranslationsModule,
    RenderHelperService,
    I18N_CONFIG,
    I18nConfig,
} from 'ngx-edu-sharing-ui';
import { RenderComponent, RenderingServiceLibModule } from 'ngx-rendering-service-lib';
import { environment } from '../environments/environment';

@NgModule({
    declarations: [AppComponent],
    imports: [
        BrowserModule,
        EduSharingApiModule.forRoot({ rootUrl: environment.eduSharingApiUrl }),
        EduSharingUiModule.forRoot({ production: true, isEmbedded: true }),
        RenderComponent,
        TranslationsModule,
        RenderingServiceLibModule.forRoot({
            assetsUrl:
                (window as any).__env?.EDU_SHARING_API_URL +
                '/web-components/rendering-service/assets',
        }),
    ],
    providers: [
        provideHttpClient(),
        // we do not read the user profile since we don't have a repository user present in lms contexts
        { provide: I18N_CONFIG, useValue: { readUserProfile: false } as I18nConfig },
        RenderHelperService,
    ],
})
export class AppModule implements DoBootstrap {
    constructor(injector: Injector) {
        const embeddedApp = createCustomElement(AppComponent, { injector });
        customElements.define('edu-sharing-render', embeddedApp);
    }

    // eslint-disable-next-line @angular-eslint/no-empty-lifecycle-method
    ngDoBootstrap(appRef: ApplicationRef): void {
        // Do nothing.
    }
}
