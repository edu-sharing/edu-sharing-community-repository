declare let __webpack_public_path__: string;

__webpack_public_path__ = (window as any).__EDUSHARING_PUBLIC_PATH__ || '';

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';

import 'zone.js';

/**
if (environment.production) {
    enableProdMode();
}

platformBrowserDynamic()
    .bootstrapModule(AppModule)
    .catch((err) => console.error(err));

*/
export function init() {
    platformBrowserDynamic()
        .bootstrapModule(AppModule)
        .catch((err) => console.error(err));
}

init();
