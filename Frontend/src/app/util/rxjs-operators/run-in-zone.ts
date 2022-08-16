// From https://stackoverflow.com/questions/50928645/running-an-observable-into-a-zone-js#57452361

import { Observable, OperatorFunction } from 'rxjs';
import { NgZone } from '@angular/core';

export function runInZone<T>(zone: NgZone): OperatorFunction<T, T> {
    return (source) => {
        return new Observable((observer) => {
            const onNext = (value: T) => zone.run(() => observer.next(value));
            const onError = (e: any) => zone.run(() => observer.error(e));
            const onComplete = () => zone.run(() => observer.complete());
            return source.subscribe(onNext, onError, onComplete);
        });
    };
}
