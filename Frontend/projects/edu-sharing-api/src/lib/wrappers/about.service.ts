import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { shareReplay } from 'rxjs/operators';
import { AboutService as AboutApiService } from '../api/services';
import { About } from '../models';

@Injectable({
    providedIn: 'root',
})
export class AboutService {
    private readonly about$;

    constructor(private about: AboutApiService) {
        this.about$ = this.about.about().pipe(shareReplay(1));
    }

    getAbout(): Observable<About> {
        return this.about$;
    }
}
