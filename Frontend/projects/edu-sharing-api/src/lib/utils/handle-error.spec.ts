import { fakeAsync, tick } from '@angular/core/testing';
import * as rxjs from 'rxjs';
import { handleError } from './handle-error';
import { switchReplay } from './switch-replay';

describe('handleError', () => {
    let defaultErrorHandler: jasmine.Spy;

    beforeEach(() => {
        defaultErrorHandler = jasmine.createSpy('defaultErrorHandler');
    });

    it('should not be called when there is no error', fakeAsync(() => {
        const observable = rxjs.of('foo').pipe(handleError(defaultErrorHandler));
        observable.subscribe();
        tick();
        expect(defaultErrorHandler).not.toHaveBeenCalled();
    }));

    it('should be called when there is an error', fakeAsync(() => {
        const observable = rxjs
            .throwError({ message: 'foo' })
            .pipe(handleError(defaultErrorHandler));
        observable.subscribe({
            error: () => void 0,
        });
        tick();
        expect(defaultErrorHandler).toHaveBeenCalled();
    }));

    it('should not be called when preventDefault was called', fakeAsync(() => {
        const observable = rxjs
            .throwError({ message: 'foo' })
            .pipe(handleError(defaultErrorHandler));
        observable.subscribe({
            error: (err) => err.preventDefault(),
        });
        tick();
        expect(defaultErrorHandler).not.toHaveBeenCalled();
    }));

    it('should not be called when there is no error when being used more than once', fakeAsync(() => {
        const observable = rxjs
            .of('foo')
            .pipe(handleError(defaultErrorHandler), handleError(defaultErrorHandler));
        observable.subscribe();
        tick();
        expect(defaultErrorHandler).not.toHaveBeenCalled();
    }));

    it('should be called when there is an error when being used more than once', fakeAsync(() => {
        const observable = rxjs
            .throwError({ message: 'foo' })
            .pipe(handleError(defaultErrorHandler), handleError(defaultErrorHandler));
        observable.subscribe({
            error: () => void 0,
        });
        tick();
        expect(defaultErrorHandler).toHaveBeenCalledTimes(2);
    }));

    it('should not be called when preventDefault was called when being used more than once', fakeAsync(() => {
        const observable = rxjs
            .throwError({ message: 'foo' })
            .pipe(handleError(defaultErrorHandler), handleError(defaultErrorHandler));
        observable.subscribe({
            error: (err) => err.preventDefault(),
        });
        tick();
        expect(defaultErrorHandler).not.toHaveBeenCalled();
    }));

    it('should be called with switchReplay', fakeAsync(() => {
        const source = rxjs.throwError({ message: 'foo' }).pipe(handleError(defaultErrorHandler));
        const trigger = rxjs.of(void 0);
        const observable = trigger.pipe(switchReplay(() => source));
        observable.subscribe({
            error: () => void 0,
        });
        tick();
        expect(defaultErrorHandler).toHaveBeenCalled();
    }));

    it('should prevent default with switchReplay', fakeAsync(() => {
        const source = rxjs.throwError({ message: 'foo' }).pipe(handleError(defaultErrorHandler));
        const trigger = rxjs.of(void 0);
        const observable = trigger.pipe(switchReplay(() => source));
        observable.subscribe({
            error: (err) => {
                err.preventDefault();
                setTimeout(() => {
                    expect(defaultErrorHandler).not.toHaveBeenCalled();
                }, 100);
            },
        });
        tick(100);
    }));

    it('should prevent default with switchReplay with multiple subscribers', fakeAsync(() => {
        const source = rxjs.throwError({ message: 'foo' }).pipe(handleError(defaultErrorHandler));
        const trigger = rxjs.of(void 0);
        const observable = trigger.pipe(switchReplay(() => source));
        observable.subscribe({
            error: (err) => {
                err.preventDefault();
            },
        });
        observable.subscribe({
            error: () => void 0,
        });
        tick();
        expect(defaultErrorHandler).not.toHaveBeenCalled();
    }));
});
