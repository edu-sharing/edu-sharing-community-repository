import * as rxjs from 'rxjs';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export function onSubscription<T>({
    onSubscribe,
    onUnsubscribe,
}: {
    onSubscribe?: () => void;
    onUnsubscribe?: () => void;
}): rxjs.MonoTypeOperatorFunction<T> {
    return (source$: Observable<T>) => {
        return new Observable((subscriber) => {
            const destroyed$ = new Subject<void>();
            onSubscribe?.();
            source$.pipe(takeUntil(destroyed$)).subscribe(subscriber);
            return () => {
                onUnsubscribe?.();
                destroyed$.next();
                destroyed$.complete();
            };
        });
    };
}
