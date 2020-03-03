import { TestBed } from '@angular/core/testing';
import { TestScheduler } from 'rxjs/Rx';
import { CardService } from './card.service';


describe('CardService', () => {
    let service: CardService;
    let scheduler: TestScheduler;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(CardService);
        scheduler = new TestScheduler((actual, expected) => {
            expect(actual).toEqual(expected);
        });
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('number modal cards', () => {
        it('should default to 0', () => {
            scheduler.run(({ expectObservable }) => {
                expectObservable(service.numberModalCards).toBe('a', { a: 0 });
            });
        });

        it('should set new values', () => {
            service.setNumberModalCards(23);
            service.setNumberModalCards(42);
            scheduler.run(({ expectObservable }) => {
                expectObservable(service.numberModalCards).toBe('a', {
                    a: 42,
                });
            });
        });
    });
});
