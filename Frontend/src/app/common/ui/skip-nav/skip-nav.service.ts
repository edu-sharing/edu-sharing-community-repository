import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

// Define as object, so we can derive the type `SkipTarget` and the values `skipTargets` from the
// same source.
const skipTargets_ = {
    'MAIN_CONTENT': {},
};

export type SkipTarget = keyof typeof skipTargets_;
const skipTargets: SkipTarget[] = Object.keys(skipTargets_) as SkipTarget[];

/**
 * Manages targets for skip navigation.
 *
 * For internal use by `SkipNavComponent` and `SkipTargetDirective`.
 */
@Injectable({
    providedIn: 'root',
})
export class SkipNavService {
    private elements: { [target in SkipTarget]?: HTMLElement } = {};
    private availableTargetsSubject = new BehaviorSubject<SkipTarget[]>([]);

    constructor() {}

    register(target: SkipTarget, element: HTMLElement): void {
        if (this.elements[target]) {
            throw new Error(`Tried to register skip target ${target}, but was already registered.`);
        }
        if (!element.hasAttribute('tabindex')) {
            element.addEventListener('blur', (event) =>
                (event.target as HTMLElement).removeAttribute('tabindex'),
            );
        }
        this.elements[target] = element;
        this.updateAvailableTargets();
    }

    unregister(target: SkipTarget): void {
        this.elements[target] = null;
        this.updateAvailableTargets();
    }

    getAvailableTargets(): Observable<SkipTarget[]> {
        return this.availableTargetsSubject.asObservable();
    }

    skipTo(target: SkipTarget): void {
        const element = this.elements[target];
        if (element) {
            if (!element.hasAttribute('tabindex')) {
                element.setAttribute('tabindex', '-1');
            }
            element.focus();
        }
    }

    private updateAvailableTargets(): void {
        const availableTargets: SkipTarget[] = [];
        for (const target of skipTargets) {
            if (this.elements[target]) {
                availableTargets.push(target);
            }
        }
        Promise.resolve().then(() => this.availableTargetsSubject.next(availableTargets));
    }
}
