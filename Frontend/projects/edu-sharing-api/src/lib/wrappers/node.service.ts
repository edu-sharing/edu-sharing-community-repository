import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NodeV1Service } from '../api/services';
import { Node } from '../models';

@Injectable({
    providedIn: 'root',
})
export class NodeService {
    constructor(private nodeV1: NodeV1Service) {}

    getNode(repository: string, id: string): Observable<Node> {
        return this.nodeV1
            .getMetadata({
                repository,
                node: id,
                propertyFilter: ['-all-'],
            })
            .pipe(map((nodeEntry) => nodeEntry.node));
    }
}
