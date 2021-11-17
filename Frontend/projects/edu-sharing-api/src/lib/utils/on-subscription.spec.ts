import { Subject } from 'rxjs';
import { take } from 'rxjs/operators';
import { onSubscription } from './on-subscription';

describe('onSubscription', () => {
    let subject: Subject<any>;

    beforeEach(() => {
        subject = new Subject();
    });

    it('should call onSubscribe', () => {
        const onSubscribe = jasmine.createSpy('onSubscribe');
        const observable = subject.pipe(onSubscription({ onSubscribe }));
        expect(onSubscribe.calls.any()).toBe(false);
        observable.subscribe();
        expect(onSubscribe.calls.any()).toBe(true);
    });

    it('should call onUnsubscribe', () => {
        const onUnsubscribe = jasmine.createSpy('onUnsubscribe');
        const observable = subject.pipe(onSubscription({ onUnsubscribe }));
        expect(onUnsubscribe.calls.any()).toBe(false);
        const subscription = observable.subscribe();
        expect(onUnsubscribe.calls.any()).toBe(false);
        subscription.unsubscribe();
        expect(onUnsubscribe.calls.any()).toBe(true);
    });

    it('should pass on a value', (done) => {
        const observable = subject.pipe(onSubscription({}));
        observable.subscribe((value) => {
            expect(value).toBe('foo');
            done();
        });
        subject.next('foo');
    });

    it('should pass on completion', (done) => {
        const observable = subject.pipe(onSubscription({}));
        observable.subscribe({
            next: (value) => expect(value).toBe('foo'),
            complete: () => done(),
        });
        subject.next('foo');
        subject.complete();
    });

    it('should pass on an error', (done) => {
        const observable = subject.pipe(onSubscription({}));
        observable.subscribe({
            error: (value) => {
                expect(value).toBe('foo');
                done();
            },
        });
        subject.error('foo');
    });

    it('should unsubscribe from source', () => {
        const observable = subject.pipe(onSubscription({}));
        expect(subject.observers.length).toBe(0);
        observable.pipe(take(2)).subscribe();
        expect(subject.observers.length).toBe(1);
        subject.next('foo');
        expect(subject.observers.length).toBe(1);
        subject.next('bar');
        expect(subject.observers.length).toBe(0);
    });
});
