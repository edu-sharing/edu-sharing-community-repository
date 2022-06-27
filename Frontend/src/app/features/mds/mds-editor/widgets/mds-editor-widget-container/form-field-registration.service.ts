import { Injectable } from '@angular/core';
import { MatFormField } from '@angular/material/form-field';

// This is a Service-Directive combination to get hold of the `MatFormField` before it initializes
// its `FormFieldControl`.
//
// This is needed so we can pass inputs (representing a `FormFieldControl`) through `<ng-content>`
// into a `MatFormField` (see https://github.com/angular/components/issues/9411).
//
// Initialization of the `FormFieldControl` happens on `ngAfterContentInit` in `MatFormField`, so we
// will be too late if we were to do this on `ngAfterViewInit`. Therefore, we hook into
// `MatFormField` with a Directive and register our `FormFieldControl` in the constructor, which is
// early enough to be picked up.
@Injectable()
export class FormFieldRegistrationService {
    private callback: (formField: MatFormField) => void;

    register(formField: MatFormField): void {
        this.callback(formField);
    }

    onRegister(callback: (formField: MatFormField) => void): void {
        this.callback = callback;
    }
}
