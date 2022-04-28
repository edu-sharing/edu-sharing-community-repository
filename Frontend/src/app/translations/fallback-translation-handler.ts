import { MissingTranslationHandler, MissingTranslationHandlerParams } from '@ngx-translate/core';

export class FallbackTranslationHandler implements MissingTranslationHandler {
    handle(params: MissingTranslationHandlerParams) {
        return (params.interpolateParams as any)?.fallback ?? params.key;
    }
}
