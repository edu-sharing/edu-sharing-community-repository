import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';
import { MdsV1Service } from '../api/services';
import { SortV2 } from '../model-overrides/sort-v-2';
import { MdsDefinition, MdsView, MetadataSetInfo } from '../models';

const DEFAULT = '-default-';
const HOME_REPOSITORY = '-home-';

export interface MdsIdentifier {
    repository: string;
    metadataSet: string;
}

@Injectable({
    providedIn: 'root',
})
export class MdsService {
    private readonly availableSetsDict: {
        [repository: string]: Observable<MetadataSetInfo[] | null>;
    } = {};
    private readonly mdsDict: { [mds: string]: Observable<MdsDefinition> } = {};

    constructor(private mdsV1: MdsV1Service) {}

    getAvailableMetadataSets(
        repository: string = HOME_REPOSITORY,
    ): Observable<MetadataSetInfo[] | null> {
        if (!(repository in this.availableSetsDict)) {
            this.availableSetsDict[repository] = this.mdsV1
                .getMetadataSetsV2({
                    repository,
                })
                .pipe(
                    map((data) => (data.metadatasets as MetadataSetInfo[]) ?? null),
                    shareReplay(),
                );
        }
        return this.availableSetsDict[repository];
    }

    getMetadataSet({
        repository = HOME_REPOSITORY,
        metadataSet = DEFAULT,
    }: Partial<MdsIdentifier>): Observable<MdsDefinition> {
        const dictKey = this.getMdsDictKey({ repository, metadataSet });
        if (!(dictKey in this.mdsDict)) {
            this.mdsDict[dictKey] = this.mdsV1
                .getMetadataSetV2({
                    repository,
                    metadataset: metadataSet,
                })
                .pipe(
                    map(
                        (mdsDefinition) =>
                            ({
                                ...mdsDefinition,
                                views: mdsDefinition.views as MdsView[],
                                sorts: mdsDefinition.sorts as SortV2[],
                            } as MdsDefinition),
                    ),
                    shareReplay(),
                );
        }
        return this.mdsDict[dictKey];
    }

    private getMdsDictKey({ repository, metadataSet }: MdsIdentifier): string {
        return `${repository}/${metadataSet}`;
    }
}
