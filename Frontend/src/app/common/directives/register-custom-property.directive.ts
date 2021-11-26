import { Directive, Input } from '@angular/core';

/**
 * Add an arbitrary property to any object.
 */
@Directive({
    selector: '[esRegisterCustomProperty]',
})
export class RegisterCustomPropertyDirective {
    @Input('esRegisterCustomProperty') set property(p: { key: string; value: any; object: any }) {
        p.object[p.key] = p.value;
    }

    constructor() {}
}
