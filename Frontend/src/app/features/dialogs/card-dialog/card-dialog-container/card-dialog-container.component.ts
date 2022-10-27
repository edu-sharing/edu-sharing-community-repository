import { animate, AnimationEvent, state, style, transition, trigger } from '@angular/animations';
import {
    ConfigurableFocusTrap,
    ConfigurableFocusTrapFactory,
    FocusMonitor,
} from '@angular/cdk/a11y';
import { _getFocusedElementPierceShadowDom } from '@angular/cdk/platform';
import { CdkPortalOutlet, ComponentPortal } from '@angular/cdk/portal';
import { DOCUMENT } from '@angular/common';
import {
    Component,
    ComponentRef,
    ElementRef,
    EventEmitter,
    HostBinding,
    HostListener,
    Inject,
    OnDestroy,
    OnInit,
    Optional,
    ViewChild,
} from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DialogButton } from '../../../../core-module/core.module';
import { UIAnimation } from '../../../../core-module/ui/ui-animation';
import { CardDialogConfig, Closable } from '../card-dialog-config';
import { CardDialogRef } from '../card-dialog-ref';
import { SavingState } from '../card-dialog-state';

let idCounter = 0;

// A lot of this code is copied from
// https://github.com/angular/components/blob/13.3.x/src/material/dialog/dialog-container.ts.

/** Event that captures the state of dialog container animations. */
interface DialogAnimationEvent {
    state: 'opened' | 'opening' | 'closing' | 'closed';
    totalTime: number;
}

type CardState = 'void' | 'enter' | 'exit';

@Component({
    selector: 'es-card-dialog-container',
    templateUrl: './card-dialog-container.component.html',
    styleUrls: ['./card-dialog-container.component.scss'],
    animations: [
        trigger('defaultAnimation', [
            state('void, exit', style({ opacity: 0, transform: 'scale(0.7)' })),
            state('enter', style({ transform: 'none' })),
            transition('* => enter', [
                style({
                    transform: 'translateY(-15%)',
                    opacity: 0,
                }),
                animate(
                    UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease',
                    style({ transform: 'translateY(0)', opacity: 1 }),
                ),
            ]),
            transition('* => void, * => exit', [
                style({ opacity: 1 }),
                animate(
                    UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease',
                    style({
                        transform: 'scale(.95)',
                        opacity: 0,
                    }),
                ),
            ]),
        ]),
        trigger('mobileAnimation', [
            state('void, exit', style({ opacity: 0, transform: 'scale(0.7)' })),
            state('enter', style({ transform: 'none' })),
            transition('* => enter', [
                style({
                    transform: 'translateY(15%)',
                    opacity: 0,
                }),
                animate(
                    UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease',
                    style({ transform: 'translateY(0)', opacity: 1 }),
                ),
            ]),
            transition('* => void, * => exit', [
                style({ opacity: 1 }),
                animate(
                    UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease',
                    style({
                        transform: 'translateY(15%)',
                        opacity: 0,
                    }),
                ),
            ]),
        ]),
    ],
})
export class CardDialogContainerComponent implements OnInit, OnDestroy {
    readonly id = idCounter++;
    @HostBinding('attr.aria-modal') readonly ariaModal = 'true';
    @HostBinding('attr.role') readonly role = 'dialog';
    @HostBinding('class') readonly class = 'mat-elevation-z24';
    @HostBinding('class.card-dialog-mobile') isMobile: boolean;
    // Make the container focusable, so keyboard shortcuts keep working when the user clicked some
    // non-interactive element.
    @HostBinding('attr.tabindex') readonly tabIndex = '-1';
    @HostBinding('attr.aria-labelledby') readonly ariaLabelledby = `card-dialog-title-${this.id}`;
    @HostBinding('attr.aria-describedby')
    readonly ariaDescribedby = `card-dialog-subtitle-${this.id}`;
    @HostBinding('@defaultAnimation') defaultAnimation: CardState | null = null;
    @HostBinding('@mobileAnimation') mobileAnimation: CardState | null = null;

    @ViewChild(CdkPortalOutlet, { static: true }) portalOutlet: CdkPortalOutlet;

    config: CardDialogConfig<unknown> = {};
    buttons: DialogButton[];
    isLoading = false;
    savingState: SavingState = null;

    /** Emits when an animation state changes. */
    readonly animationStateChanged = new EventEmitter<DialogAnimationEvent>();

    // Cannot be injected because the dialogRef is available only after this container is created.
    // Will be set right after construction.
    dialogRef!: CardDialogRef<unknown, unknown>;

    private focusTrap: ConfigurableFocusTrap;
    /** Element that was focused before the dialog was opened. Save this to restore upon close. */
    private elementFocusedBeforeDialogWasOpened: HTMLElement | null = null;
    private readonly destroyed$ = new Subject<void>();

    constructor(
        // @Inject(CARD_DIALOG_STATE) private dialogState: CardDialogState,
        // @Inject(CARD_DIALOG_OVERLAY_REF) private overlayRef: OverlayRef,
        @Optional() @Inject(DOCUMENT) private document: any,
        private elementRef: ElementRef<HTMLElement>,
        private focusTrapFactory: ConfigurableFocusTrapFactory,
        private focusMonitor?: FocusMonitor,
    ) {}

    ngOnInit(): void {
        this.dialogRef
            .observeConfig()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((cardConfig) =>
                Promise.resolve().then(() => {
                    this.config = cardConfig;
                    this.updateButtons();
                }),
            );
        this.dialogRef
            .observeState('viewMode')
            .pipe(takeUntil(this.destroyed$))
            .subscribe((viewMode) => (this.isMobile = viewMode === 'mobile'));
        this.dialogRef
            .observeState('isLoading')
            .pipe(takeUntil(this.destroyed$))
            .subscribe((isLoading) => {
                this.updateButtons();
                this.isLoading = isLoading;
            });
        this.dialogRef
            .observeState('savingState')
            .pipe(takeUntil(this.destroyed$))
            .subscribe((savingState) => {
                this.updateButtons();
                this.savingState = savingState;
            });
        this.setState('enter');
    }

    private updateButtons(): void {
        if (this.dialogRef.state.isLoading || this.dialogRef.state.savingState === 'saving') {
            this.buttons = this.config.buttons?.map((button) => ({
                ...button,
                disabled: true,
            }));
        } else {
            this.buttons = this.config.buttons;
        }
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        this.dialogRef = null;
    }

    attachComponentPortal<T>(portal: ComponentPortal<T>): ComponentRef<T> {
        return this.portalOutlet.attachComponentPortal(portal);
    }

    initializeWithAttachedContent() {
        this.focusTrap = this.focusTrapFactory.create(this.elementRef.nativeElement);

        // Save the previously focused element. This element will be re-focused
        // when the dialog closes.
        if (this.document) {
            this.elementFocusedBeforeDialogWasOpened = _getFocusedElementPierceShadowDom();
        }
        this.trapFocus();
    }

    @HostListener('@defaultAnimation.start', ['$event'])
    @HostListener('@mobileAnimation.start', ['$event'])
    onAnimationStart({ toState, totalTime }: AnimationEvent) {
        if (toState === 'enter') {
            this.animationStateChanged.next({ state: 'opening', totalTime });
        } else if (toState === 'exit' || toState === 'void') {
            this.animationStateChanged.next({ state: 'closing', totalTime });
        }
    }

    @HostListener('@defaultAnimation.done', ['$event'])
    @HostListener('@mobileAnimation.done', ['$event'])
    onAnimationDone({ toState, totalTime }: AnimationEvent) {
        if (toState === 'enter') {
            // if (this._config.delayFocusTrap) {
            //     this._trapFocus();
            // }
            this.animationStateChanged.next({ state: 'opened', totalTime });
        } else if (toState === 'exit') {
            this.restoreFocus();
            this.animationStateChanged.next({ state: 'closed', totalTime });
        }
    }

    onCloseButtonClick(): void {
        this.dialogRef.tryCancel('x-button');
    }

    shouldShowCloseButton(): boolean {
        return this.config.closable <= Closable.Confirm;
    }

    /** Starts the dialog exit animation. */
    startExitAnimation(): void {
        this.setState('exit');

        // Mark the container for check so it can react if the
        // view container is using OnPush change detection.
        // this._changeDetectorRef.markForCheck();
    }

    private setState(state: CardState): void {
        switch (this.dialogRef.state.viewMode) {
            case 'default':
                this.defaultAnimation = state;
                break;
            case 'mobile':
                this.mobileAnimation = state;
                break;
        }
    }

    /** Restores focus to the element that was focused before the dialog opened. */
    private restoreFocus() {
        const previousElement = this.elementFocusedBeforeDialogWasOpened;

        // We need the extra check, because IE can set the `activeElement` to null in some cases.
        if (
            // this._config.restoreFocus &&
            previousElement &&
            typeof previousElement.focus === 'function'
        ) {
            const activeElement = _getFocusedElementPierceShadowDom();
            const element = this.elementRef.nativeElement;

            // Make sure that focus is still inside the dialog or is on the body (usually because a
            // non-focusable element like the backdrop was clicked) before moving it. It's possible that
            // the consumer moved it themselves before the animation was done, in which case we shouldn't
            // do anything.
            if (
                !activeElement ||
                activeElement === this.document.body ||
                activeElement === element ||
                element.contains(activeElement)
            ) {
                if (this.focusMonitor) {
                    //   this._focusMonitor.focusVia(previousElement, this._closeInteractionType);
                    this.focusMonitor.focusVia(previousElement, 'program');
                    //   this._closeInteractionType = null;
                } else {
                    previousElement.focus();
                }
            }
        }

        if (this.focusTrap) {
            this.focusTrap.destroy();
        }
    }

    trapFocus() {
        // Ensure that focus is on the dialog container. It's possible that a different
        // component tried to move focus while the open animation was running. See:
        // https://github.com/angular/components/issues/16215. Note that we only want to do this
        // if the focus isn't inside the dialog already, because it's possible that the consumer
        // turned off `autoFocus` in order to move focus themselves.
        if (!this.containsFocus()) {
            this.focusContainer();
        }
    }

    /** Returns whether focus is inside the dialog. */
    private containsFocus() {
        const element = this.elementRef.nativeElement;
        const activeElement = _getFocusedElementPierceShadowDom();
        return element === activeElement || element.contains(activeElement);
    }

    private focusContainer(): void {
        const element = this.elementRef.nativeElement;
        element.focus();
    }
}
