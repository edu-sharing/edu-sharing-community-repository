import { Injectable, inject } from '@angular/core';
import { RelationV1Service } from '../api/services/relation-v-1.service';
import { HOME_REPOSITORY } from '../constants';
import { Observable } from 'rxjs';
import { NodeRelationData } from '../api/models/node-relation-data';

@Injectable({
    providedIn: 'root',
})
export class RelationService {
    private relationV1Service = inject(RelationV1Service);

    getRelations(
        nodeId: string,
        { repository = HOME_REPOSITORY } = {},
    ): Observable<NodeRelationData[]> {
        return this.relationV1Service.getRelations({
            node: nodeId,
            repository: repository,
        });
    }
    createRelation(
        source: string,
        target: string,
        type: 'isPartOf' | 'isBasedOn' | 'references',
        { repository = HOME_REPOSITORY } = {},
    ): Observable<any> {
        return this.relationV1Service.createRelation({
            body: {
                fromNode: source,
                toNode: target,
                type: type,
            },
            repository: repository,
        });
    }
    deleteRelation(
        source: string,
        target: string,
        type: 'isPartOf' | 'isBasedOn' | 'references',
        { repository = HOME_REPOSITORY } = {},
    ): Observable<any> {
        return this.relationV1Service.deleteRelation({
            source,
            target,
            type,
            repository: repository,
        });
    }
}
