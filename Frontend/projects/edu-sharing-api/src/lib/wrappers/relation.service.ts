import { Injectable } from '@angular/core';
import { RelationV1Service } from '../api/services/relation-v-1.service';
import { HOME_REPOSITORY } from '../constants';

@Injectable({
    providedIn: 'root',
})
export class RelationService {
    constructor(private relationV1Service: RelationV1Service) {}
    getRelations(nodeId: string, { repository = HOME_REPOSITORY } = {}) {
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
    ) {
        return this.relationV1Service.createRelation({
            source,
            target,
            type,
            repository: repository,
        });
    }
    deleteRelation(
        source: string,
        target: string,
        type: 'isPartOf' | 'isBasedOn' | 'references',
        { repository = HOME_REPOSITORY } = {},
    ) {
        return this.relationV1Service.deleteRelation({
            source,
            target,
            type,
            repository: repository,
        });
    }
}
