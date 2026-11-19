import { HttpClient } from '@angular/common/http';
import { ModuleWithProviders, NgModule, Optional, inject } from '@angular/core';
import {
    MissingTranslationHandler,
    TranslateLoader,
    provideTranslateService,
} from '@ngx-translate/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';
import { FallbackTranslationHandler } from './fallback-translation-handler';
import { TranslationLoader } from './translation-loader';
import { I18N_CONFIG } from '../types/injection-tokens';

/**
 * Import this module once in the app module to provide the `TranslateService`.
 *
 * Import `TranslatePipe` and `TranslateDirective` where translations are used in templates.
 */
@NgModule({
    declarations: [],
    providers: [
        provideTranslateService({
            loader: {
                provide: TranslateLoader,
                useFactory: TranslationLoader.create,
                deps: [
                    HttpClient,
                    ConfigService,
                    EduSharingUiConfiguration,
                    [new Optional(), I18N_CONFIG],
                ],
            },
            missingTranslationHandler: {
                provide: MissingTranslationHandler,
                useClass: FallbackTranslationHandler,
            },
        }),
    ],
})
export class TranslationsModule {
    static forRoot(): ModuleWithProviders<TranslationsModule> {
        return {
            ngModule: TranslationsModule,
            providers: [],
        };
    }

    constructor() {
        const parentModule = inject(TranslationsModule, { optional: true, skipSelf: true });

        if (parentModule) {
            console.warn(
                'TranslationsModule is already loaded. Import it in the AppModule only',
                parentModule,
            );
            /*throw new Error(
                'TranslationsModule is already loaded. Import it in the AppModule only',
            );*/
        }
    }
}
