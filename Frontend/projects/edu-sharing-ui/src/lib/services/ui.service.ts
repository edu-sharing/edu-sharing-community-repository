import { Injectable, NgZone } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { UIConstants } from '../util/ui-constants';
import { OptionItem } from '../types/option-item';
import { distinctUntilChanged, map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class UIService {
    private isTouchSubject = new BehaviorSubject(false);
    private metaKeyPressedSubject = new BehaviorSubject(false);
    private shiftKeyPressedSubject = new BehaviorSubject(false);
    private ctrlKeyPressedSubject = new BehaviorSubject(false);

    get shiftKeyPressed() {
        return this.shiftKeyPressedSubject.value;
    }

    constructor(private ngZone: NgZone) {
        // HostListener not working, so use window
        this.ngZone.runOutsideAngular(() => {
            window.addEventListener('keydown', (event) => {
                this.onKeyDownOrKeyUp(event);
            });
            window.addEventListener('keyup', (event) => {
                this.onKeyDownOrKeyUp(event);
            });
            window.addEventListener('pointerdown', (event) => {
                // Usually, properties for modifier keys will be set correctly on keydown and keyup
                // events, but there are situations where the operating system intercepts key
                // presses, e.g. the Windows key on Linux systems, so we update again on mouse
                // clicks to be sure.
                this.updateModifierKeys(event);
            });
            window.addEventListener('pointerdown', (event) => {
                // Usually, properties for modifier keys will be set correctly on keydown and keyup
                // events, but there are situations where the operating system intercepts key
                // presses, e.g. the Windows key on Linux systems, so we update again on mouse
                // clicks to be sure.
                const isTouch = (event as PointerEvent).pointerType === 'touch';
                if (this.isTouchSubject.value !== isTouch) {
                    this.ngZone.run(() => this.isTouchSubject.next(isTouch));
                }
            });
        });
    }
    private onKeyDownOrKeyUp(event: KeyboardEvent) {
        // `event.metaKey` is not consistent across browsers on the actual keypress of the modifier
        // key. So we handle these events separately.
        if (event.key === 'Control') {
            this.ctrlKeyPressedSubject.next(event.type === 'keydown');
        } else if (event.key === 'Shift') {
            this.shiftKeyPressedSubject.next(event.type === 'keydown');
        } else if (event.key === 'Meta') {
            this.metaKeyPressedSubject.next(event.type === 'keydown');
        } else {
            // In case we miss modifier events because the browser didn't have focus during the
            // event, we update modifier keys on unrelated key events as well.
            this.updateModifierKeys(event);
        }
    }

    private updateModifierKeys(event: PointerEvent | KeyboardEvent) {
        this.metaKeyPressedSubject.next(event.metaKey);
        this.shiftKeyPressedSubject.next(event.shiftKey);
        this.ctrlKeyPressedSubject.next(event.ctrlKey);
    }

    observeCtrlOrCmdKeyPressedOutsideZone(): Observable<boolean> {
        return rxjs.combineLatest([this.metaKeyPressedSubject, this.ctrlKeyPressedSubject]).pipe(
            map(([metaKeyPressed, ctrlKeyPressed]) => metaKeyPressed || ctrlKeyPressed),
            distinctUntilChanged(),
        );
    }

    public isMobile() {
        return this.isTouchSubject.value;
    }
    public static evaluateMediaQuery(type: string, value: number) {
        if (type == UIConstants.MEDIA_QUERY_MAX_WIDTH) return value > window.innerWidth;
        if (type == UIConstants.MEDIA_QUERY_MIN_WIDTH) return value < window.innerWidth;
        if (type == UIConstants.MEDIA_QUERY_MAX_HEIGHT) return value > window.innerHeight;
        if (type == UIConstants.MEDIA_QUERY_MIN_HEIGHT) return value < window.innerHeight;
        console.warn('Unsupported media query ' + type);
        return true;
    }
    filterValidOptions(options: OptionItem[]) {
        if (options == null) return null;
        options = options.filter((value) => value != null);
        let optionsFiltered: OptionItem[] = [];
        for (let option of options) {
            if (
                (!option.onlyMobile || (option.onlyMobile && this.isMobile())) &&
                (!option.onlyDesktop || (option.onlyDesktop && !this.isMobile())) &&
                (!option.mediaQueryType ||
                    (option.mediaQueryType &&
                        UIService.evaluateMediaQuery(
                            option.mediaQueryType,
                            option.mediaQueryValue,
                        )))
            )
                optionsFiltered.push(option);
        }
        return optionsFiltered;
    }
}
