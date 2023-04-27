import { fakeAsync, tick } from '@angular/core/testing';
import * as rxjs from 'rxjs';
import { Observable, Subject } from 'rxjs';
import { first, startWith, switchMap, take, tap } from 'rxjs/operators';
import { switchReplay } from './switch-replay';

describe('switchReplay', () => {
    let spy: jasmine.Spy;

    beforeEach(() => {
        spy = jasmine.createSpy('spy');
    });

    it('should keep back old values', fakeAsync(() => {
        let value = 'foo';
        const source$ = new Observable((subscriber) => {
            setTimeout(() => {
                subscriber.next(value);
                subscriber.complete();
            }, 10);
        });
        const trigger$ = new Subject<void>();
        const observable$ = trigger$.pipe(
            startWith(void 0 as void),
            switchReplay(() => source$),
        );
        observable$.subscribe(() => spy());
        tick(11);
        value = 'bar';
        trigger$.next();
        observable$.subscribe((v) => expect(v).toBe('bar'));
        tick(11);
        expect(spy).toHaveBeenCalledTimes(2);
    }));

    it('should trigger source again', fakeAsync(() => {
        const source$ = new Observable((subscriber) => {
            spy();
            subscriber.next();
            subscriber.complete();
        });
        const trigger$ = new Subject<void>();
        const observable$ = trigger$.pipe(
            startWith(void 0 as void),
            switchReplay(() => source$),
        );
        observable$.subscribe();
        tick();
        trigger$.next();
        tick();
        expect(spy).toHaveBeenCalledTimes(2);
    }));

    it('should forward error', fakeAsync(() => {
        const source$ = new Observable((subscriber) => {
            spy();
            subscriber.error();
        });
        const trigger$ = new Subject<void>();
        const observable$ = trigger$.pipe(
            startWith(void 0 as void),
            switchReplay(() => source$),
        );
        trigger$.next();
        observable$.subscribe({
            next: () => fail('expected error'),
            error: () => void 0,
        });
        tick();
        expect(spy).toHaveBeenCalledTimes(1);
    }));

    it('should forward error to two observables', fakeAsync(() => {
        let isError = false;
        let errorCount = 0;
        const source$ = new Observable((subscriber) => {
            if (isError) {
                spy();
                subscriber.error();
            } else {
                subscriber.next();
                subscriber.complete();
            }
        });
        const trigger$ = new Subject<void>();
        const observable$ = trigger$.pipe(
            startWith(void 0 as void),
            switchReplay(() => source$),
        );
        observable$.subscribe({
            error: () => errorCount++,
        });
        tick();
        isError = true;
        trigger$.next();
        observable$.subscribe({
            next: () => fail('expected error'),
            error: () => errorCount++,
        });
        tick();
        expect(errorCount).toEqual(2);
        expect(spy).toHaveBeenCalledTimes(1);
    }));

    it('should unsubscribe from source', fakeAsync(() => {
        const source$ = new Observable((subscriber) => {
            spy();
            subscriber.next();
            subscriber.complete();
        });
        const trigger$ = new Subject<void>();
        const observable$ = trigger$.pipe(
            startWith(void 0 as void),
            switchReplay(() => source$),
        );
        observable$.pipe(first()).subscribe();
        tick();
        trigger$.next();
        tick();
        expect(spy).toHaveBeenCalledTimes(1);
    }));

    it('should complete when trigger completes', fakeAsync(() => {
        let completed = false;
        const source$ = new Observable((subscriber) => {
            spy();
            subscriber.next();
            subscriber.complete();
        });
        const trigger$ = new Subject<void>();
        const observable$ = trigger$.pipe(
            startWith(void 0 as void),
            switchReplay(() => source$),
        );
        observable$.subscribe({
            complete: () => (completed = true),
        });
        tick();
        trigger$.complete();
        tick();
        expect(spy).toHaveBeenCalledTimes(1);
        expect(completed).toBe(true);
    }));

    it('should share error', fakeAsync(() => {
        let errorCount = 0;
        const source$ = new Observable((subscriber) => {
            spy();
            subscriber.error();
        });
        const trigger$ = new Subject<void>();
        const observable$ = trigger$.pipe(switchReplay(() => source$));
        observable$.subscribe({
            error: () => errorCount++,
        });
        observable$.subscribe({
            next: () => fail('expected error'),
            error: () => errorCount++,
        });
        tick();
        trigger$.next();
        tick();
        expect(errorCount).toEqual(2);
        expect(spy).toHaveBeenCalledTimes(1);
    }));

    it('should give values to new subscribers after error', fakeAsync(() => {
        let isError = true;
        let errorCount = 0;
        let successCount = 0;
        const source$ = new Observable((subscriber) => {
            spy();
            if (isError) {
                subscriber.error();
            } else {
                subscriber.next();
                subscriber.complete();
            }
        });
        const trigger$ = new Subject<void>();
        const observable$ = trigger$.pipe(
            startWith(void 0 as void),
            switchReplay(() => source$),
        );
        observable$.subscribe({
            error: () => errorCount++,
        });
        tick();
        isError = false;
        trigger$.next();
        observable$.subscribe(() => successCount++);
        tick();
        expect(errorCount).toEqual(1, 'error count');
        expect(successCount).toEqual(1, 'success count');
        expect(spy).toHaveBeenCalledTimes(2);
    }));

    it('should repeat the value after unsubscribe', fakeAsync(() => {
        let successCount = 0;
        const source$ = new Observable((subscriber) => {
            spy();
            subscriber.next();
            subscriber.complete();
        });
        const trigger$ = new Subject<void>();
        const observable$ = trigger$.pipe(
            startWith(void 0 as void),
            switchReplay(() => source$),
        );
        observable$.pipe(first()).subscribe(() => successCount++);
        tick();
        observable$.pipe(first()).subscribe(() => successCount++);
        tick();
        expect(successCount).toEqual(2, 'success count');
        expect(spy).toHaveBeenCalledTimes(1);
    }));

    it('should resolve projected value after complete', fakeAsync(() => {
        const source = rxjs.of('foo');
        const trigger = rxjs.of(null);
        const observable = trigger.pipe(switchReplay(() => source));
        observable.subscribe((value) => {
            expect(value).toEqual('foo');
            spy();
        });
        tick();
        expect(spy).toHaveBeenCalledTimes(1);
    }));

    it('should resolve projected error after complete', fakeAsync(() => {
        const source = rxjs.throwError(null);
        const trigger = rxjs.of(null);
        const observable = trigger.pipe(switchReplay(() => source));
        observable.subscribe({
            error: () => spy(),
        });
        tick();
        expect(spy).toHaveBeenCalledTimes(1);
    }));

    it('should forward complete from trigger', fakeAsync(() => {
        const source = rxjs.of(null);
        const trigger = rxjs.timer(0, 100).pipe(take(2));
        const observable = trigger.pipe(switchReplay(() => source));
        observable.subscribe({
            complete: () => spy(),
        });
        tick(110);
        expect(spy).toHaveBeenCalledTimes(1);
    }));

    it('should forward error from trigger', fakeAsync(() => {
        const source = rxjs.of(null);
        const trigger = rxjs
            .timer(0, 100)
            .pipe(switchMap((c) => (c ? rxjs.throwError(null) : rxjs.of(c))));
        const observable = trigger.pipe(switchReplay(() => source));
        observable.subscribe({
            error: () => spy(),
        });
        tick(110);
        expect(spy).toHaveBeenCalledTimes(1);
    }));
});
