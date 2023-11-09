import { ApplicationRef, DoBootstrap, Injector, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';

import { createCustomElement } from '@angular/elements';
import { AppModule as EduSharingModule } from 'src/app/app.module';
import { AppComponent } from '../../../../src/app/app.component';
import { LocationStrategy } from '@angular/common';
import { MockLocationStrategy } from '@angular/common/testing';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { environment } from '../environments/environment';

@NgModule({
    declarations: [AppComponent],
    imports: [
        BrowserModule,
        EduSharingModule,
        EduSharingApiModule.forRoot({ rootUrl: environment.eduSharingApiUrl }),
    ],
    providers: [
        {
            provide: LocationStrategy,
            useClass: MockLocationStrategy,
        },
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
