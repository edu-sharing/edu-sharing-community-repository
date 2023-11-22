import { LocationStrategy } from '@angular/common';
import { MockLocationStrategy } from '@angular/common/testing';
import { ApplicationRef, DoBootstrap, Injector, NgModule } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { BrowserModule } from '@angular/platform-browser';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { ASSETS_BASE_PATH, EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { AppModule as EduSharingModule } from 'src/app/app.module';
import { environment } from '../environments/environment';
import { AppComponent } from './app.component';

@NgModule({
    declarations: [AppComponent],
    imports: [
        BrowserModule,
        EduSharingModule,
        EduSharingApiModule.forRoot({ rootUrl: environment.eduSharingApiUrl }),
        EduSharingUiModule.forRoot({ production: environment.production, isEmbedded: true }),
    ],
    providers: [
        {
            provide: LocationStrategy,
            useClass: MockLocationStrategy,
        },
        { provide: ASSETS_BASE_PATH, useValue: 'edu-sharing/' },
    ],
})
// export class AppModule {}
export class AppModule implements DoBootstrap {
    constructor(injector: Injector) {
        const embeddedApp = createCustomElement(AppComponent, { injector });
        customElements.define('edu-sharing-app', embeddedApp);
    }

    // eslint-disable-next-line @angular-eslint/no-empty-lifecycle-method
    ngDoBootstrap(appRef: ApplicationRef): void {
        // Do nothing.
    }
}
