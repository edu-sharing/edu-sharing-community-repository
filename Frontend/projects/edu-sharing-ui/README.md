# EduSharingUi

This library bundles base ui components from edu-sharing.

This library was generated with [Angular CLI](https://github.com/angular/angular-cli) version 13.3.0.

## Required Init tasks

Your project must provide a Toast service (for message viewing). You may use a mat-snackbar for displaying.

in your app.module.ts:

```ts
import { Toast as ToastAbstract } from 'ngx-edu-sharing-ui';
providers: [
    { provide: ToastAbstract, useClass: Toast },
    // ...
];
```

Make sure that your `Toast`-Service extends the abstract Toast service.

You might also provide other services for extended functionality, including
`Toast`, `OptionsHelperService`, `KeyboardShortcutsService`, `AppService`.

Check out our sample angular application for a full example
Check the `app.module.ts` for registration.

In one of your primary components (i.e. router) you need to initalize the material theme and translations:

```ts
async ngOnInit() {
  this.materialCssVarsService.setPrimaryColor('#2e9e9b');
  this.materialCssVarsService.setAccentColor('#2e9e9b');
  await this.translationsService.initialize().toPromise()
}
```
