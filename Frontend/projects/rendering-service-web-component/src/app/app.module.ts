import {
    ApplicationRef,
    DoBootstrap,
    Injectable,
    Injector,
    NgModule,
    Provider,
    inject,
} from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { AppComponent } from './app.component';
import { BrowserModule } from '@angular/platform-browser';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import {
    EduSharingUiModule,
    I18N_CONFIG,
    I18nConfig,
    RenderHelperService,
    TranslationsModule,
} from 'ngx-edu-sharing-ui';
import {
    PdfComponent,
    RenderComponent,
    RenderingServiceLibModule,
} from 'ngx-rendering-service-lib';
import { environment } from '../environments/environment';
import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import { MatButtonModule } from '@angular/material/button';
import { RenderingServiceApiModule } from 'ngx-rendering-service-api';
import { Toast as ToastAbstract } from 'ngx-edu-sharing-ui';

@Injectable({ providedIn: 'root' })
export abstract class Toast extends ToastAbstract {
    toast(message: string, translationParameters?: any) {}

    error(errorObject: any, message?: string, translationParameters?: any) {}
}

@NgModule({
    declarations: [AppComponent],
    imports: [
        BrowserModule,
        RenderingServiceApiModule.forRoot({}),
        // no credentials mode since we're fetching only public data from the repository
        EduSharingApiModule.forRoot({
            rootUrl: environment.eduSharingApiUrl,
            withCredentials: false,
        }),
        EduSharingUiModule.forRoot({ production: true, isEmbedded: true }),
        RenderComponent,
        MatButtonModule,
        CdkConnectedOverlay,
        CdkOverlayOrigin,
        TranslationsModule,
        RenderingServiceLibModule.forRoot({
            assetsUrl: environment.production
                ? (window as any).__env?.EDU_SHARING_API_URL +
                  '/../web-components/rendering-service/assets'
                : '/vendor/assets',
        }),
        PdfComponent,
    ],
    providers: [
        provideHttpClient(withInterceptorsFromDi()),
        // we do not read the user profile since we don't have a repository user present in lms contexts
        { provide: I18N_CONFIG, useValue: { readUserProfile: false } as I18nConfig },
        { provide: ToastAbstract, useClass: Toast },
        RenderHelperService,
    ] as Provider[],
})
export class AppModule implements DoBootstrap {
    constructor() {
        const injector = inject(Injector);

        const embeddedApp = createCustomElement(AppComponent, { injector });
        customElements.define('edu-sharing-render', embeddedApp);
    }

    // eslint-disable-next-line @angular-eslint/no-empty-lifecycle-method
    ngDoBootstrap(appRef: ApplicationRef): void {
        // Do nothing.
    }
}
