import {Directive, ElementRef} from '@angular/core';
import {FormControlName, NgControl} from '@angular/forms';

/**
 * this directive injects nativeElement to all bound fields for FormBuilder
 * So the nativeElement is accessible for focusing on validation
 */
@Directive({
    // tslint:disable-next-line:directive-selector
    selector: '[formControlName]',
})
export class NativeElementInjectorDirective {
    constructor(private el: ElementRef, private control : FormControlName) {
        // timeout because control will be attached later
        setTimeout(() => {
            if(control.control) {
                (control.control as any).nativeElement = el.nativeElement;
            }
        });
    }
}
