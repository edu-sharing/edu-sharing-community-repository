import 'zone.js';
import { createApplication } from '@angular/platform-browser';
import { createCustomElement } from '@angular/elements';
import { PdfComponent } from './pdf.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ApiHelpersService } from 'ngx-edu-sharing-api';
import { importProvidersFrom } from '@angular/core';
import { RenderingServiceApiModule } from 'ngx-rendering-service-api';
import { TranslateModule } from '@ngx-translate/core';

void (async () => {
    const app = await createApplication({
        providers: [
            ApiHelpersService,
            importProvidersFrom(RenderingServiceApiModule.forRoot()),
            importProvidersFrom(TranslateModule.forRoot()),
            provideHttpClient(withInterceptorsFromDi()),
        ],
    });

    const element = createCustomElement(PdfComponent, {
        injector: app.injector,
    });

    customElements.define('es-pdf', element);
})();
