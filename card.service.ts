import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class CardService {
    numberModalCards: Observable<number>;

    private numberModalCardsSubject = new BehaviorSubject<number>(0);

    constructor() {
        this.numberModalCards = this.numberModalCardsSubject.asObservable();
    }

    public setNumberModalCards(n: number) {
        this.numberModalCardsSubject.next(n);
    }
}
