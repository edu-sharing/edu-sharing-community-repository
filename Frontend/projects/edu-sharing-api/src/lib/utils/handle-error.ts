import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { delay } from 'rxjs/operators';

/**
 * Adds a default error handler to the observable.
 *
 * In case the source observable throws an error, an additional property `preventDefault` will be
 * added to the error object before propagating the error further. If this method is called
 * (synchronously) by any error handler, no further action will be taken. Otherwise,
 * `defaultErrorHandler` will be called with the error object as parameter.
 *
 * Note that `defaultErrorHandler` will be called _after_ the error was propagated through.
 *
 * Example usage:
 * ```
 * const o$ = source$.pipe(
 *     handleError((err) => {
 *         console.log('All your kittens explode!');
 *     }),
 * )
 *
 * o$.subscribe({
 *     error: (err) => err.preventDefault(); // Phew, just saved a load of kittens.
 * })
 * ```
 */
export function handleError<T>(
    defaultErrorHandler: (error: any) => void,
): rxjs.MonoTypeOperatorFunction<T> {
    return (source$: Observable<T>) => {
        return new Observable((subscriber) => {
            let defaultPrevented = false;
            source$.subscribe({
                next: (value) => subscriber.next(value),
                complete: () => subscriber.complete(),
                error: (err) => {
                    if (!err.preventDefault) {
                        err.preventDefault = () => (defaultPrevented = true);
                        Object.defineProperty(err, 'defaultPrevented', {
                            get: () => defaultPrevented,
                        });
                    }
                    subscriber.error(err);
                    rxjs.of(null)
                        // Wait two ticks in case `handleError` was used in combination with
                        // `switchReplay`, which takes a tick to forward errors.
                        .pipe(delay(0), delay(0))
                        .subscribe(() => {
                            if (!err.defaultPrevented) {
                                defaultErrorHandler(err);
                            }
                        });
                },
            });
        });
    };
}
