import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable, Subject } from 'rxjs';
import { distinctUntilChanged, finalize, first } from 'rxjs/operators';
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
     * @param until An abort criterion for the task. Usually a task finishes when its creator calls
     * `done` on it. This parameter signals a state, where the creator is no longer in the position
     * to call `done`. When the task was created by a component, `until` should emit when the
     * component is destroyed. The effect of `until` emitting and `done` being called is the same,
     * but since forgetting to call `done` is a common mistake, we require the additional value.
     */
    addLoadingTask({
        startup = true,
        until,
    }: {
        startup?: boolean;
        until: Observable<void>;
    }): LoadingTask {
        const doneSubject = new Subject<void>();
        const loadingTask = new LoadingTask(() => {
            doneSubject.next();
            doneSubject.complete();
        });
        rxjs.merge(until, doneSubject)
            .pipe(first())
            .subscribe(() => this.onLoadingTaskDone(loadingTask));
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
        params: Parameters<LoadingScreenService['addLoadingTask']>[0],
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
