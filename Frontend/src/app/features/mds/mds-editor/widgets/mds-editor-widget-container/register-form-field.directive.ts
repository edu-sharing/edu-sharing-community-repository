import { Directive } from '@angular/core';
import { MatFormField } from '@angular/material/form-field';
import { FormFieldRegistrationService } from './form-field-registration.service';

@Directive({
    selector: '[esRegisterFormField]',
})
export class RegisterFormFieldDirective {
    constructor(formField: MatFormField, formFieldRegistration: FormFieldRegistrationService) {
        formFieldRegistration.register(formField);
    }
}
