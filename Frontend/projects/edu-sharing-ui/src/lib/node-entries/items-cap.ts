import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

export class ItemsCap<T> {
    /** The number of items to which the data should be capped. */
    get cap(): number | null {
        return this._cap.value;
    }
    set cap(value: number | null) {
        this._cap.next(value ?? null);
    }
    private _cap = new BehaviorSubject<number | null>(null);

    /** Whether to temporarily disable capping. */
    get disabled(): boolean {
        return this._disabled.value;
    }
    set disabled(value: boolean) {
        this._disabled.next(value);
    }
    private _disabled = new BehaviorSubject(false);

    /** Whether there is more data available that is currently being capped. */
    get isActivelyCapping(): boolean {
        return this._isActivelyCapping;
    }
    private _isActivelyCapping = false;

    private _effectiveCap: Observable<number | null> = rxjs
        .combineLatest([this._cap, this._disabled])
        .pipe(
            map(([cap, disabled]) => (disabled ? null : cap)),
            distinctUntilChanged(),
        );

    connect(dataStream: Observable<T[]>): Observable<T[]> {
        return rxjs.combineLatest([this._effectiveCap, dataStream]).pipe(
            map(([effectiveCap, originalData]) => {
                const needToCap = this._needToCap(effectiveCap, originalData);
                this._isActivelyCapping = needToCap;
                if (needToCap) {
                    return originalData.slice(0, effectiveCap);
                } else {
                    return originalData;
                }
            }),
        );
    }

    private _needToCap(effectiveCap: number | null, originalData: T[]): boolean {
        return effectiveCap && originalData?.length > effectiveCap;
    }
}
