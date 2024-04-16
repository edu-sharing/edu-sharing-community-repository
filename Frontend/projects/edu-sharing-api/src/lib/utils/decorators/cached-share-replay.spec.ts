import { fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';
import { cachedShareReplay, KeyCache } from './cached-share-replay';

describe('cachedShareReplay', () => {
    let spy: jasmine.Spy;
    let foo: Foo;

    beforeEach(() => {
        spy = jasmine.createSpy('spy');
        foo = new Foo();
    });

    it('should call the original function', fakeAsync(() => {
        foo.identity('foo').subscribe((v) => expect(v).toBe('foo'));
        tick();
        expect(foo.callCount).toBe(1);
    }));

    it('should call the original function only once', fakeAsync(() => {
        foo.identity('foo', () => 'foo').subscribe((v) => expect(v).toBe('foo'));
        foo.identity('foo', () => 'bar').subscribe((v) => expect(v).toBe('foo'));
        tick();
        expect(foo.callCount).toBe(1);
    }));

    it('should respect different keys', fakeAsync(() => {
        foo.identity('foo').subscribe((v) => expect(v).toBe('foo'));
        foo.identity('bar').subscribe((v) => expect(v).toBe('bar'));
        tick();
        expect(foo.callCount).toBe(2);
    }));

    it('should reset a key', fakeAsync(() => {
        let value = 'foo';
        foo.identity('foo', () => value)
            .pipe(take(1))
            .subscribe((v) => expect(v).toBe('foo'));
        tick();
        Foo.cache.reset('foo');
        tick();
        value = 'bar';
        foo.identity('foo').subscribe((v) => expect(v).toBe('bar'));
        tick();
        expect(foo.callCount).toBe(2);
    }));
});

class Foo {
    static readonly cache = new KeyCache();

    callCount = 0;

    constructor() {
        (Foo.cache['_data'] as KeyCache['_data']) = {};
    }

    @cachedShareReplay(Foo.cache, (s: string) => s)
    identity(s: string, getValue = () => s) {
        return new Observable((subscriber) => {
            this.callCount++;
            setTimeout(() => {
                subscriber.next(getValue());
                subscriber.complete();
            });
        });
    }
}
