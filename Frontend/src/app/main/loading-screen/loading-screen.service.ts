import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { distinctUntilChanged, finalize } from 'rxjs/operators';
import { LoadingScreenComponent } from './loading-screen.component';

class LoadingTask {
    get isDone() {
        return this._isDone;
    }
    private _isDone = false;

    constructor(private onDone: (loadingTask: LoadingTask) => void) {}

    done(): void {
        if (this._isDone) {
            console.warn('Called `done` on a loading task more than once.');
        } else {
            this.onDone(this);
            this._isDone = true;
        }
    }
}

@Injectable({
    providedIn: 'root',
})
export class LoadingScreenService {
    private overlayRef: OverlayRef;
    private isLoadingSubject = new rxjs.BehaviorSubject(true);
    private loadingTasks: LoadingTask[] = [];

    constructor(private overlay: Overlay) {
        this.isLoadingSubject.pipe(distinctUntilChanged()).subscribe((isLoading) => {
            if (isLoading) {
                this.show();
            } else {
                this.hide();
            }
        });
    }

    getIsLoading(): boolean {
        return this.isLoadingSubject.value;
    }

    observeIsLoading(): Observable<boolean> {
        return this.isLoadingSubject.asObservable();
    }

    /**
     * Adds a new loading task that will keep the loading spinner until done.
     *
     * @param startup Any new tasks with `startup: true` that are added after initial loading
     * finished will be ignored (default: true)
     */
    addLoadingTask({ startup = true } = {}): LoadingTask {
        const loadingTask = new LoadingTask(this.onLoadingTaskDone.bind(this));
        if (!startup || this.getIsLoading()) {
            this.loadingTasks.push(loadingTask);
            this.isLoadingSubject.next(true);
        }
        return loadingTask;
    }

    /**
     * Adds a new loading task to an observable pipe.
     *
     * The task is registered at the time the pipe is created and will be marked as done when the
     * observable either completes or throws an error.
     * 
     * Use this function with observables that emit once and then complete.
     * 
     * @param params See `addLoadingTask`
     */
    showUntilFinished<T>(
        params: Parameters<LoadingScreenService['addLoadingTask']>[0] = {},
    ): rxjs.UnaryFunction<Observable<T>, Observable<T>> {
        const task = this.addLoadingTask(params);
        return rxjs.pipe(finalize(() => task.done()));
    }

    private onLoadingTaskDone(loadingTask: LoadingTask): void {
        const index = this.loadingTasks.indexOf(loadingTask);
        if (index >= 0) {
            this.loadingTasks.splice(index, 1);
        }
        if (this.loadingTasks.length === 0) {
            this.isLoadingSubject.next(false);
        }
    }

    private show(): void {
        this.overlayRef = this.overlay.create();
        const userProfilePortal = new ComponentPortal(LoadingScreenComponent);
        this.overlayRef.attach(userProfilePortal);
    }

    private hide(): void {
        this.overlayRef.detach();
        this.overlayRef.dispose();
    }
}
