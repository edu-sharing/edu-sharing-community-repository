import { Pipe, PipeTransform } from '@angular/core';
import { Observable, of, pipe, UnaryFunction } from 'rxjs';
import { catchError, map, startWith } from 'rxjs/operators';

export type WrappedResponse<T> =
    | { state: 'loading' }
    | { state: 'success'; data: T }
    | { state: 'error'; error: any };

export function wrapResponse<T>(): UnaryFunction<Observable<T>, Observable<WrappedResponse<T>>> {
    return pipe(
        map((data) => ({ state: 'success', data } as WrappedResponse<T>)),
        catchError((error) => of({ state: 'error', error } as WrappedResponse<T>)),
        startWith({ state: 'loading' } as WrappedResponse<T>),
    );
}

@Pipe({
    name: 'wrapObservable',
    standalone: true,
})
export class WrapObservablePipe implements PipeTransform {
    transform<T>(value: Observable<T>): Observable<WrappedResponse<T>> {
        return value.pipe(wrapResponse());
    }
}
