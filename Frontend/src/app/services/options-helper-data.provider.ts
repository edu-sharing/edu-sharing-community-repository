import { inject, Provider } from '@angular/core';
import { OptionsHelperDataService } from 'ngx-edu-sharing-ui';

/**
 * Reuse an ancestor-provided OptionsHelperDataService when one exists (so children share a single
 * options/selection channel), otherwise create a component-local instance. Use in a component's
 * `providers` instead of listing `OptionsHelperDataService` directly.
 */
export function provideReusableOptionsHelperData(): Provider {
    return {
        provide: OptionsHelperDataService,
        useFactory: () =>
            inject(OptionsHelperDataService, { optional: true, skipSelf: true }) ??
            new OptionsHelperDataService(),
    };
}
