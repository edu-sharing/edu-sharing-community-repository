import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';
import * as apiModels from '../api/models';
import { ConfigV1Service } from '../api/services';

export type Variables = apiModels.Variables['current'];

@Injectable({
    providedIn: 'root',
})
export class ConfigService {
    private readonly variables$ = this.configV1.getVariables().pipe(
        map((variables) => variables.current ?? null),
        shareReplay(1),
    );

    constructor(private configV1: ConfigV1Service) {}

    getVariables(): Observable<Variables | null> {
        return this.variables$;
    }
}
