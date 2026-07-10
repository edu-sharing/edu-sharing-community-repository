import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const fromBeforeToValidator: ValidatorFn = (
    group: AbstractControl,
): ValidationErrors | null => {
    const from = group.get('from');
    const to = group.get('to');
    if (
        from?.enabled &&
        to?.enabled &&
        from.value != null &&
        to.value != null &&
        from.value > to.value
    ) {
        return { fromAfterTo: true };
    }
    return null;
};

export const notInPastValidator: ValidatorFn = (
    control: AbstractControl,
): ValidationErrors | null => {
    if (control.dirty && control.value != null && control.value < Date.now()) {
        return { inPast: true };
    }
    return null;
};
