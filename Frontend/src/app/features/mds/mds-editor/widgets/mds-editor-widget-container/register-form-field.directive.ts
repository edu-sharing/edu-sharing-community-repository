import { Directive, inject } from '@angular/core';
import { MatFormField } from '@angular/material/form-field';
import { FormFieldRegistrationService } from './form-field-registration.service';

@Directive({
    selector: '[esRegisterFormField]',
    standalone: false,
})
export class RegisterFormFieldDirective {
    constructor() {
        const formField = inject(MatFormField);
        const formFieldRegistration = inject(FormFieldRegistrationService);

        formFieldRegistration.register(formField);
    }
}
