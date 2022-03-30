import * as rxjs from 'rxjs';
import { handleError } from './handle-error';

describe('handleError', () => {
    let defaultErrorHandler: jasmine.Spy;

    beforeEach(() => {
        defaultErrorHandler = jasmine.createSpy('defaultErrorHandler');
    });

    it('should not be called when there is no error', () => {
        const observable = rxjs.of('foo').pipe(handleError(defaultErrorHandler));
        observable.subscribe();
        expect(defaultErrorHandler).not.toHaveBeenCalled();
    });

    it('should be called when there is an error', (done) => {
        const observable = rxjs
            .throwError({ message: 'foo' })
            .pipe(handleError(defaultErrorHandler));
        observable.subscribe({
            error: () => done(),
        });
        expect(defaultErrorHandler).toHaveBeenCalled();
    });

    it('should not be called when preventDefault was called', (done) => {
        const observable = rxjs
            .throwError({ message: 'foo' })
            .pipe(handleError(defaultErrorHandler));
        observable.subscribe({
            error: (err) => {
                err.preventDefault();
                done();
            },
        });
        expect(defaultErrorHandler).not.toHaveBeenCalled();
    });

    it('should not be called when there is no error when being used more than once', () => {
        const observable = rxjs
            .of('foo')
            .pipe(handleError(defaultErrorHandler), handleError(defaultErrorHandler));
        observable.subscribe();
        expect(defaultErrorHandler).not.toHaveBeenCalled();
    });

    it('should be called when there is an error when being used more than once', (done) => {
        const observable = rxjs
            .throwError({ message: 'foo' })
            .pipe(handleError(defaultErrorHandler), handleError(defaultErrorHandler));
        observable.subscribe({
            error: () => done(),
        });
        expect(defaultErrorHandler).toHaveBeenCalledTimes(2);
    });

    it('should not be called when preventDefault was called when being used more than once', (done) => {
        const observable = rxjs
            .throwError({ message: 'foo' })
            .pipe(handleError(defaultErrorHandler), handleError(defaultErrorHandler));
        observable.subscribe({
            error: (err) => {
                err.preventDefault();
                done();
            },
        });
        expect(defaultErrorHandler).not.toHaveBeenCalled();
    });
});
