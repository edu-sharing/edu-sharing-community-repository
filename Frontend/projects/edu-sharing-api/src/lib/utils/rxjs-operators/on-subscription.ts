import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';

/**
 * Provides hooks to subscribe- and unsubscribe events on the observable.
 */
export function onSubscription<T>({
    onSubscribe,
    onUnsubscribe,
}: {
    onSubscribe?: () => void;
    onUnsubscribe?: () => void;
}): rxjs.MonoTypeOperatorFunction<T> {
    return (source$: Observable<T>) => {
        return new Observable((subscriber) => {
            onSubscribe?.();
            source$.subscribe(subscriber);
            return () => {
                onUnsubscribe?.();
            };
        });
    };
}
