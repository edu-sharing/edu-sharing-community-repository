# Translations

Depending on the application mode, translations are loaded locally or are provided by the backend.

-   In **development mode**, translations are loaded from `assets/i18n/<language>.json`)
-   In **production mode**, translations are provided by the backend.

## Usage

If possible, use the `translate` directive:

```html
<div translate [translateParams]="{value: 'world'}">HELLO</div>
```

... or the `translate` pipe:

```html
<div>{{ 'HELLO' | translate:{value: 'world'} }}</div>
```

You can also translate strings programmatically:

```ts
import { TranslateService } from "@ngx-translate/core";
import { TranslationsService } from '../translations/translations.service';

constructor(
    private translate: TranslateService,
    private translations: TranslationsService,
) {
    // This is the preferred way of programmatically using translations.
    this.translate.get('HELLO', {value: 'world'}).subscribe((translatedString) => {
        // Do stuff.
    });

    // Do this only if you have to use `translate.instant()`.
    this.translations.waitForInit().subscribe(() => {
        // You can safely use `this.translate.instant()` here.
    });
}
```

See https://github.com/ngx-translate/core for more information.
