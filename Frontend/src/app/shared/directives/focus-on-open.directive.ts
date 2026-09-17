import { Directive, ElementRef, effect, inject, input } from '@angular/core';
import { InputModalityDetector } from '@angular/cdk/a11y';

/**
 * Moves focus into the host when it opens, and back out when it closes — for a panel whose
 * trigger doesn't sit next to it in DOM order (e.g. portaled, or a Material drawer reprojected
 * to the end of its container). Only reacts to keyboard activation, so mouse users don't get an
 * unexpected focus jump.
 */
@Directive({
    selector: '[esFocusOnOpen]',
    standalone: false,
})
export class FocusOnOpenDirective {
    private elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
    private inputModalityDetector = inject(InputModalityDetector);

    /** whether the host panel is currently open */
    readonly open = input.required<boolean>({ alias: 'esFocusOnOpen' });
    /**
     * Fallback restore target, used only when there was no meaningful prior focus to capture
     * (e.g. a programmatic open). A function, since the target may live in a CDK overlay and can
     * only be resolved lazily.
     */
    readonly returnFocusTo = input<
        HTMLElement | ElementRef<HTMLElement> | (() => HTMLElement | null) | null
    >(null);

    /** Element focused right before the panel opened; the primary restore target on close. */
    private previouslyFocused: HTMLElement | null = null;

    constructor() {
        let wasOpen = false;
        effect(() => {
            const isOpen = this.open();
            // capture before the DOM reflects the new state — Chrome blurs to <body> asynchronously
            const focusWasInside = this.elementRef.nativeElement.contains(document.activeElement);
            if (isOpen && !wasOpen) {
                const active = document.activeElement;
                // document.body means nothing was meaningfully focused (e.g. a programmatic open)
                this.previouslyFocused =
                    active instanceof HTMLElement &&
                    active !== document.body &&
                    !this.elementRef.nativeElement.contains(active)
                        ? active
                        : null;
                if (this.inputModalityDetector.mostRecentModality === 'keyboard') {
                    this.elementRef.nativeElement.focus();
                }
            } else if (!isOpen && wasOpen && focusWasInside) {
                // prefer the captured element; fall back if it's gone (e.g. list re-rendered) or unset
                const restoreTarget = this.previouslyFocused?.isConnected
                    ? this.previouslyFocused
                    : this.resolveReturnTarget();
                restoreTarget?.focus();
                this.previouslyFocused = null;
            }
            wasOpen = isOpen;
        });
    }

    private resolveReturnTarget(): HTMLElement | null {
        const target = this.returnFocusTo();
        if (typeof target === 'function') {
            return target();
        }
        if (target instanceof ElementRef) {
            return target.nativeElement;
        }
        return target;
    }
}
