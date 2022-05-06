import { OverlayRef, OverlaySizeConfig } from '@angular/cdk/overlay';
import { Observable, Subject } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { CardDialogContainerComponent } from './card-dialog-container/card-dialog-container.component';

// A lot of this code is copied from
// https://github.com/angular/components/blob/13.3.x/src/material/dialog/dialog-ref.ts.

export class CardDialogRef<R> {
    private readonly afterClosedSubject = new Subject<R | undefined>();
    /** Handle to the timeout that's running as a fallback in case the exit animation doesn't fire. */
    private closeFallbackTimeout: ReturnType<typeof setTimeout>;
    private result: R;

    constructor(
        private overlayRef: OverlayRef,
        private containerInstance: CardDialogContainerComponent,
    ) {
        containerInstance.animationStateChanged
            .pipe(
                filter((event) => event.state === 'closed'),
                take(1),
            )
            .subscribe(() => {
                clearTimeout(this.closeFallbackTimeout);
                this.finishDialogClose();
            });
        containerInstance.triggerClose.subscribe(() => this.close());
        this.overlayRef.detachments().subscribe(() => {
            this.afterClosedSubject.next(this.result);
            this.afterClosedSubject.complete();
            // this.componentInstance = null!;
        });
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

    private finishDialogClose(): void {
        this.overlayRef.dispose();
    }
}
