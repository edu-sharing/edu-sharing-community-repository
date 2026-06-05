import { FocusableOption } from '@angular/cdk/a11y';
import { Directive, ElementRef, Input, OnDestroy, OnInit, inject } from '@angular/core';
import { Subject } from 'rxjs';

@Directive({
    selector: '[esWorkspaceShareFocusableOption]',
    standalone: false,
})
export class FocusableOptionDirective implements FocusableOption, OnInit, OnDestroy {
    private _elementRef = inject<ElementRef<HTMLElement>>(ElementRef);

    @Input() disabled?: boolean;
    @Input() customFocusFunction?: () => void;

    readonly focused = new Subject<FocusableOptionDirective>();

    private _onFocus = () => this.focused.next(this);

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
