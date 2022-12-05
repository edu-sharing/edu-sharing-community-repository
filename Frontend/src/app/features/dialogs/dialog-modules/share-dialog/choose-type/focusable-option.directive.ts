import { FocusableOption } from '@angular/cdk/a11y';
import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';

@Directive({
    selector: '[esWorkspaceShareFocusableOption]',
})
export class FocusableOptionDirective implements FocusableOption, OnInit, OnDestroy {
    @Input() disabled?: boolean;
    @Input() customFocusFunction?: () => void;

    readonly focused = new Subject<FocusableOptionDirective>();

    private _onFocus = () => this.focused.next(this);

    constructor(private _elementRef: ElementRef<HTMLElement>) {}

    ngOnInit(): void {
        this._elementRef.nativeElement.addEventListener('focus', this._onFocus);
    }

    ngOnDestroy(): void {
        this._elementRef.nativeElement.removeEventListener('focus', this._onFocus);
    }

    focus() {
        if (this.customFocusFunction) {
            this.customFocusFunction();
        } else {
            this._elementRef.nativeElement.focus();
        }
    }
}
