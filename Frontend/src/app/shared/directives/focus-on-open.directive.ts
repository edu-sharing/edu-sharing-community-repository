import { DestroyRef, Directive, ElementRef, effect, inject, input } from '@angular/core';
import { InputModalityDetector } from '@angular/cdk/a11y';

/**
 * Moves focus into the host when it opens, and back out when it closes — for a panel whose
 * trigger doesn't sit next to it in DOM order (e.g. portaled, or a Material drawer reprojected
 * to the end of its container). Only reacts to keyboard activation, so mouse users don't get an
 * unexpected focus jump.
 *
 * A panel that stays open but shows new content (e.g. a sidebar switching from one node's
 * preview to another) can pass that content as `esFocusOnOpenContent`; a change caused by a key
 * press while focus is outside the panel moves focus in the same way as opening it. Changes made by
 * code without a recent key press (e.g. after an upload finished) leave focus where it is.
 */

/** How long after a key press a content change still counts as caused by it. */
const KEY_PRESS_WINDOW_MS = 1000;
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
     * The content the open panel shows; a new non-null value counts as opening the panel again.
     * Compared by reference.
     */
    readonly content = input<unknown>(null, { alias: 'esFocusOnOpenContent' });
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
    /** Time of the latest key press anywhere on the page. */
    private lastKeyDown = Number.NEGATIVE_INFINITY;

    constructor() {
        // a plain listener rather than a host listener, so key presses don't trigger change detection
        const onKeyDown = () => (this.lastKeyDown = performance.now());
        document.addEventListener('keydown', onKeyDown, true);
        inject(DestroyRef).onDestroy(() =>
            document.removeEventListener('keydown', onKeyDown, true),
        );

        let wasOpen = false;
        let previousContent: unknown = null;
        effect(() => {
            const isOpen = this.open();
            const content = this.content();
            // capture before the DOM reflects the new state — Chrome blurs to <body> asynchronously
            const focused = this.resolveFocusedElement();
            const focusWasInside = !!focused && this.elementRef.nativeElement.contains(focused);
            const contentChanged =
                isOpen && wasOpen && content != null && content !== previousContent;
            if (isOpen && !wasOpen) {
                this.focusHost(focused, false);
            } else if (
                contentChanged &&
                !focusWasInside &&
                performance.now() - this.lastKeyDown < KEY_PRESS_WINDOW_MS
            ) {
                this.focusHost(focused, true);
            } else if (!isOpen && wasOpen && focusWasInside) {
                // prefer the captured element; fall back if it's gone (e.g. list re-rendered) or unset
                const restoreTarget = this.previouslyFocused?.isConnected
                    ? this.previouslyFocused
                    : this.resolveReturnTarget();
                restoreTarget?.focus();
                this.previouslyFocused = null;
            }
            wasOpen = isOpen;
            previousContent = content;
        });
    }

    /**
     * Remembers `focused` as restore target and, after keyboard use, focuses the host. With
     * `keepTarget`, an earlier target is kept when `focused` is no meaningful target.
     */
    private focusHost(focused: HTMLElement | null, keepTarget: boolean): void {
        // document.body means nothing was meaningfully focused (e.g. a programmatic open)
        const target =
            focused && focused !== document.body && !this.elementRef.nativeElement.contains(focused)
                ? focused
                : null;
        if (target || !keepTarget) {
            this.previouslyFocused = target;
        }
        if (this.inputModalityDetector.mostRecentModality === 'keyboard') {
            this.elementRef.nativeElement.focus();
        }
    }

    /**
     * The focused element, with a menu item of an open overlay menu standing for the menu's
     * trigger: the item is removed once the menu closes, and the trigger tells whether the menu
     * was opened from inside the host.
     */
    private resolveFocusedElement(): HTMLElement | null {
        const active = document.activeElement;
        if (!(active instanceof HTMLElement)) {
            return null;
        }
        if (active.closest('.cdk-overlay-container')) {
            const trigger = document.querySelector<HTMLElement>(
                '[aria-haspopup="menu"][aria-expanded="true"]',
            );
            if (trigger) {
                return trigger;
            }
        }
        return active;
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
