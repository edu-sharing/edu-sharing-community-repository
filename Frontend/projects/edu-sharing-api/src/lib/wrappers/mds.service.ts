import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { shareReplay } from 'rxjs/operators';
import { MdsV2 } from '../api/models';
import { MdsV1Service } from '../api/services';

export interface MdsIdentifier {
    repository: string;
    metadataSet: string;
}

@Injectable({
    providedIn: 'root',
})
export class MdsService {
    private readonly mdsDict: { [mds: string]: Observable<MdsV2> } = {};

    constructor(private mdsV1: MdsV1Service) {}

    getMetadataSet({ repository, metadataSet }: MdsIdentifier): Observable<MdsV2> {
        const dictKey = this.getDictKey({ repository, metadataSet });
        if (!(dictKey in this.mdsDict)) {
            this.mdsDict[dictKey] = this.mdsV1
                .getMetadataSetV2({
                    repository,
                    metadataset: metadataSet,
                })
                .pipe(shareReplay());
        }
        return this.mdsDict[dictKey];
    }

    private getDictKey({ repository, metadataSet }: MdsIdentifier): string {
        return `${repository}/${metadataSet}`;
    }
}
