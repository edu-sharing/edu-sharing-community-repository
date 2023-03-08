import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MdsV1Service } from '../api/services';
import { DEFAULT, HOME_REPOSITORY } from '../constants';
import { MdsDefinition, MdsSort, MdsView, MetadataSetInfo } from '../models';
import { shareReplayReturnValue } from '../utils/decorators/share-replay-return-value';

export interface MdsIdentifier {
    repository: string;
    metadataSet: string;
}

@Injectable({
    providedIn: 'root',
})
export class MdsService {
    constructor(private mdsV1: MdsV1Service) {}

    @shareReplayReturnValue()
    getAvailableMetadataSets(
        repository: string = HOME_REPOSITORY,
    ): Observable<MetadataSetInfo[] | null> {
        return this.mdsV1
            .getMetadataSets({
                repository,
            })
            .pipe(map((data) => (data.metadatasets as MetadataSetInfo[]) ?? null));
    }

    /**
     * Gets the given metadata.
     *
     * NOTE: "DEFAULT" will refer to the primary metadata set of the repository but will NOT obey
     * any restrictions given via the (client-side) configuration.
     */
    @shareReplayReturnValue()
    getMetadataSet({
        repository = HOME_REPOSITORY,
        metadataSet = DEFAULT,
    }: Partial<MdsIdentifier>): Observable<MdsDefinition> {
        return this.mdsV1
            .getMetadataSet({
                repository,
                metadataset: metadataSet,
            })
            .pipe(
                map(
                    (mdsDefinition) =>
                        ({
                            ...mdsDefinition,
                            views: mdsDefinition.views as MdsView[],
                            sorts: mdsDefinition.sorts as MdsSort[],
                        } as MdsDefinition),
                ),
            );
    }
}
