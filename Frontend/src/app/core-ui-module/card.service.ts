import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class CardService {
    hasOpenModals: Observable<boolean>;

    private hasOpenModalsSubject = new BehaviorSubject<boolean>(false);

    constructor() {
        this.hasOpenModals = this.hasOpenModalsSubject.asObservable();
    }

    public setNumberModalCards(n: number) {
        this.hasOpenModalsSubject.next(n > 0);
    }
}
