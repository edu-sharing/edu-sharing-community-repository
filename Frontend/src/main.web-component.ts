import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { enableProdMode, provideZoneChangeDetection } from '@angular/core';
import { environment } from './environments/environment';
import { WebComponentModule } from './app/app.web-component.module';

if (environment.production) {
    enableProdMode();
}

platformBrowserDynamic()
    .bootstrapModule(WebComponentModule, { applicationProviders: [provideZoneChangeDetection()] })
    .catch((err) => console.error(err));
