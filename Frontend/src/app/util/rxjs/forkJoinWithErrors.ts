import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

interface SuccessResult<T> {
    kind: 'success';
    value: T;
}

interface ErrorResult {
    kind: 'error';
    error: any;
}

type Result<T> = SuccessResult<T> | ErrorResult;

function isSuccess<T>(result: Result<T>): result is SuccessResult<T> {
    return result.kind === 'success';
}

function isError<T>(result: Result<T>): result is ErrorResult {
    return result.kind === 'error';
}

/**
 * Like `forkJoin` but instead of aborting when an error occurs, continues and returns errors and
 * successful results as an array each.
 */
export function forkJoinWithErrors<T>(
    sources: Observable<T>[],
): Observable<{ successes: T[]; errors: any[] }> {
    return forkJoin(
        sources.map((source) =>
            source.pipe(
                map((value) => ({ kind: 'success' as const, value })),
                catchError((error) => of({ kind: 'error' as const, error })),
            ),
        ),
    ).pipe(
        map((results) => ({
            successes: results.filter(isSuccess).map(({ value }) => value),
            errors: results.filter(isError).map(({ error }) => error),
        })),
    );
}
