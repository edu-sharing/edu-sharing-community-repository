import { ApplicationRef, DoBootstrap, Injector, NgModule } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { AppComponent } from './app.component';
import { BrowserModule } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { EduSharingUiModule, TranslationsModule, RenderHelperService } from 'ngx-edu-sharing-ui';
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
            assetsUrl: 'node_modules/ngx-edu-sharing-rendering-web-component/assets',
        }),
    ],
    providers: [provideHttpClient(), RenderHelperService],
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
