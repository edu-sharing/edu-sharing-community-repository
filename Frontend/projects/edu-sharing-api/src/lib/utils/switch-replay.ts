import { BehaviorSubject, Observable } from 'rxjs';
import { delay, first, shareReplay, switchMap, tap } from 'rxjs/operators';

/**
 * Like `switchMap` followed by `shareReplay`, but does not emit old values to new subscribers while
 * the observable returned by `project` is in-flight.
 *
 * In other words, if an update was triggered by an emission of `source$` and a new subscriber comes
 * in, they won't get the previous value replayed. Instead, the first value they see is the result
 * of the projected observable once it emits. After that, everyone gets shared updates on newly
 * emitted values once they become available (like a normal `switchMap`-`shareReplay` combination).
 */
export function switchReplay<T, R>(project: (value: T, index: number) => Observable<R>) {
    const inFlightSubject = new BehaviorSubject(false);
    return (source$: Observable<T>) => {
        const inner$ = source$.pipe(
            tap(() => inFlightSubject.next(true)),
            switchMap((value, index) => project(value, index)),
            tap({
                next: () => inFlightSubject.next(false),
                error: () => inFlightSubject.next(false),
            }),
            shareReplay(1),
        );
        return inFlightSubject.pipe(
            first((inFlight) => !inFlight),
            delay(0),
            switchMap(() => inner$),
        );
    };
}
