import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export enum SkipTarget {
    MainContent = 'MAIN_CONTENT',
}

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
            throw new Error(
                `Tried to register skip target ${target}, but was already registered.`,
            );
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
        for (const target of Object.values(SkipTarget)) {
            if (this.elements[target]) {
                availableTargets.push(target);
            }
        }
        this.availableTargetsSubject.next(availableTargets);
    }
}
