import { Inject, Injectable, Optional } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { SkipNavConfig, SKIP_NAV_CONFIG } from './skip-nav.config';

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
    private readonly config: SkipNavConfig;

    constructor(@Optional() @Inject(SKIP_NAV_CONFIG) config: SkipNavConfig) {
        this.config = config ?? {};
    }

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
            element.focus({ preventScroll: true });
            this.scrollIntoView(element);
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

    private scrollIntoView(element: HTMLElement): void {
        const top =
            element.getBoundingClientRect().top + window.pageYOffset - this.getScrollOffset();
        window.scrollTo({
            top,
            behavior: 'smooth',
        });
    }

    private getScrollOffset(): number {
        if (typeof this.config.scrollOffset === 'number') {
            return this.config.scrollOffset;
        } else if (typeof this.config.scrollOffset === 'function') {
            return this.config.scrollOffset();
        } else {
            return 0;
        }
    }
}
