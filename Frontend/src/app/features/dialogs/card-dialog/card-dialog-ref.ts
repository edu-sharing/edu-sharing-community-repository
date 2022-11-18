import { ESCAPE, hasModifierKey } from '@angular/cdk/keycodes';
import { OverlayRef, OverlaySizeConfig } from '@angular/cdk/overlay';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { distinctUntilChanged, filter, map, take } from 'rxjs/operators';
import { DISCARD_OR_BACK } from '../dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../dialogs.service';
import { CardDialogConfig, Closable } from './card-dialog-config';
import { CardDialogContainerComponent } from './card-dialog-container/card-dialog-container.component';
import { CardDialogState } from './card-dialog-state';
// A lot of this code is copied from
// https://github.com/angular/components/blob/13.3.x/src/material/dialog/dialog-ref.ts.

export class CardDialogRef<D = unknown, R = unknown> {
    private readonly afterClosedSubject = new Subject<R | undefined>();
    /** Handle to the timeout that's running as a fallback in case the exit animation doesn't fire. */
    private closeFallbackTimeout: ReturnType<typeof setTimeout>;
    private result: R;
    private configSubject: BehaviorSubject<CardDialogConfig<D>>;
    private stateSubject: BehaviorSubject<CardDialogState>;

    get config() {
        return this.configSubject.value;
    }

    get state() {
        return this.stateSubject.value;
    }

    constructor(
        private overlayRef: OverlayRef,
        private containerInstance: CardDialogContainerComponent,
        config: CardDialogConfig<D>,
        state: CardDialogState,
        private dialogs: DialogsService,
    ) {
        this.configSubject = new BehaviorSubject(config);
        this.stateSubject = new BehaviorSubject(state);
        containerInstance.animationStateChanged
            .pipe(
                filter((event) => event.state === 'closed'),
                take(1),
            )
            .subscribe(() => {
                clearTimeout(this.closeFallbackTimeout);
                this.finishDialogClose();
            });
        this.overlayRef.detachments().subscribe(() => {
            this.afterClosedSubject.next(this.result);
            this.afterClosedSubject.complete();
            // this.componentInstance = null!;
            this.containerInstance = null;
        });
        this.registerCancel();
    }

    close(result?: R): void {
        this.result = result;
        // Transition the backdrop in parallel to the dialog.
        this.containerInstance.animationStateChanged
            .pipe(
                filter((event) => event.state === 'closing'),
                take(1),
            )
            .subscribe((event) => {
                // this._beforeClosed.next(dialogResult);
                // this._beforeClosed.complete();
                this.overlayRef.detachBackdrop();

                // The logic that disposes of the overlay depends on the exit animation completing, however
                // it isn't guaranteed if the parent view is destroyed while it's running. Add a fallback
                // timeout which will clean everything up if the animation hasn't fired within the specified
                // amount of time plus 100ms. We don't need to run this outside the NgZone, because for the
                // vast majority of cases the timeout will have been cleared before it has the chance to fire.
                this.closeFallbackTimeout = setTimeout(
                    () => this.finishDialogClose(),
                    event.totalTime + 100,
                );
            });

        // this._state = MatDialogState.CLOSING;
        this.containerInstance.startExitAnimation();
    }

    afterClosed(): Observable<R | null> {
        return this.afterClosedSubject.asObservable();
    }

    updateSize(sizeConfig: OverlaySizeConfig): void {
        this.overlayRef.updateSize(sizeConfig);
    }

    patchConfig(config: Partial<CardDialogConfig<D>>): void {
        this.configSubject.next({ ...this.configSubject.value, ...config });
    }

    patchState(state: Partial<CardDialogState>): void {
        this.stateSubject.next({ ...this.stateSubject.value, ...state });
    }

    observeConfig(): Observable<CardDialogConfig<D>> {
        return this.configSubject.asObservable();
    }

    observeState<K extends keyof CardDialogState>(property: K): Observable<CardDialogState[K]> {
        return this.stateSubject.pipe(
            map((state) => state[property]),
            distinctUntilChanged(),
        );
    }

    tryCancel(trigger: 'backdrop' | 'x-button' | 'esc-key' | 'navigation'): {
        acknowledged: boolean;
        closed: Promise<boolean>;
    } {
        switch (this.config.closable) {
            case Closable.Casual:
                this.close();
                return { acknowledged: true, closed: Promise.resolve(true) };
            case Closable.Standard:
                switch (trigger) {
                    case 'backdrop':
                        return { acknowledged: false, closed: Promise.resolve(false) };
                    case 'x-button':
                    case 'esc-key':
                    case 'navigation':
                        this.close();
                        return { acknowledged: true, closed: Promise.resolve(true) };
                }
            case Closable.Confirm:
                switch (trigger) {
                    case 'backdrop':
                        return { acknowledged: false, closed: Promise.resolve(false) };
                    case 'x-button':
                    case 'esc-key':
                    case 'navigation':
                        const closed = this.dialogs
                            .openGenericDialog({
                                title: 'DIALOG.CONFIRM_DISCARD_TITLE',
                                messageText: 'DIALOG.CONFIRM_DISCARD_MESSAGE',
                                buttons: DISCARD_OR_BACK,
                            })
                            .then((dialogRef) => dialogRef.afterClosed().toPromise())
                            .then((response) => {
                                if (response === 'DISCARD') {
                                    this.close();
                                    return true;
                                } else {
                                    return false;
                                }
                            });
                        return { acknowledged: true, closed };
                }
            case Closable.Disabled:
                return { acknowledged: false, closed: Promise.resolve(false) };
        }
    }

    private finishDialogClose(): void {
        this.overlayRef.dispose();
    }

    private registerCancel(): void {
        this.overlayRef
            .keydownEvents()
            .pipe(
                filter(() => !this.state.isLoading && this.state.autoSavingState !== 'saving'),
                filter((event) => event.keyCode === ESCAPE && !hasModifierKey(event)),
            )
            .subscribe((event) => {
                const acknowledged = this.tryCancel('esc-key').acknowledged;
                if (acknowledged) {
                    event.preventDefault();
                }
            });

        this.overlayRef
            .backdropClick()
            .pipe(filter(() => !this.state.isLoading && this.state.autoSavingState !== 'saving'))
            .subscribe(async () => {
                const closed = await this.tryCancel('backdrop').closed;
                if (!closed) {
                    // Move focus back to the dialog.
                    this.containerInstance.trapFocus();
                }
            });
    }
}
