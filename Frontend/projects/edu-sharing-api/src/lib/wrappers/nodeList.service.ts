import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable, Subject } from 'rxjs';
import { catchError, filter, finalize, map, startWith, switchMap, toArray } from 'rxjs/operators';
import { NodeEntries } from '../api/models';
import { IamV1Service } from '../api/services';
import { HOME_REPOSITORY, ME } from '../constants';
import { ApiErrorResponse } from '../models';
import { switchReplay } from '../utils/switch-replay';
import { AuthenticationService } from './authentication.service';

export interface SortPolicy {
    property: string;
    direction: 'asc' | 'desc';
}

class GetNodeListOptions {
    userId: string = ME;
    repository: string = HOME_REPOSITORY;
    propertyFilter?: string[];
    sortPolicies?: SortPolicy[];
}

export type NodeListErrorResponses = NodeListErrorResponse[];

interface NodeListErrorResponse {
    nodeId: string;
    error: ApiErrorResponse;
}

/**
 * Provides access to node lists.
 *
 * Node lists are specific to users. When no `userId` is given in the parameters, lists of the
 * currently logged in user will be used.
 */
@Injectable({
    providedIn: 'root',
})
export class NodeListService {
    /**
     * Triggers when the node list of the given listId changes.
     */
    private readonly nodeListChangesSubject = new Subject<string>();
    private readonly nodeListObservables: { [parameters: string]: Observable<NodeEntries> } = {};

    constructor(private authentication: AuthenticationService, private iamApi: IamV1Service) {}

    /**
     * Observe the node list with the given `listId`.
     *
     * The observable updates on changes to the list or when the logged in user changes.
     */
    observeNodeList(
        listId: string,
        options: Partial<GetNodeListOptions> = {},
    ): Observable<NodeEntries> {
        const params: Parameters<NodeListService['observeNodeList']> = [listId, options];
        const paramsString = JSON.stringify(params);
        if (!this.nodeListObservables[paramsString]) {
            this.nodeListObservables[paramsString] = this.createGetNodeList(params);
        }
        return this.nodeListObservables[paramsString];
    }

    /**
     * Adds the given `nodeIds` to the list with `listIds`.
     *
     * @throws `NodeListErrorResponses`
     */
    addToNodeList(
        listId: string,
        nodeIds: string[],
        {
            userId = ME,
            repository = HOME_REPOSITORY,
        }: {
            userId?: string;
            repository?: string;
        } = {},
    ): Observable<void> {
        if (nodeIds.length === 0) {
            return rxjs.of(void 0);
        }
        const observables = nodeIds.map((nodeId) =>
            this.iamApi
                .addNodeList({
                    list: listId,
                    node: nodeId,
                    person: userId,
                    repository: repository,
                })
                .pipe(
                    map(() => ({ nodeId, success: true })),
                    catchError((error) => rxjs.of({ nodeId, error })),
                ),
        );
        // TODO: replace `concat` / `toArray` with `forkJoin` when the backend can handle
        // simultaneous requests.
        return rxjs.forkJoin(...observables).pipe(
            // return rxjs.forkJoin(observables).pipe(
            // Use `finalize` instead of `tap` since even if individual nodes failed to be
            // added, others might still have gone through.
            finalize(() => this.nodeListChangesSubject.next(listId)),
        );
    }

    /**
     * Removes the given `nodeIds` from the list with `listIds`.
     */
    removeFromNodeList(
        listId: string,
        nodeIds: string[],
        {
            userId = ME,
            repository = HOME_REPOSITORY,
        }: {
            userId?: string;
            repository?: string;
        } = {},
    ): Observable<void> {
        if (nodeIds.length === 0) {
            return rxjs.of(void 0);
        }
        const observables = nodeIds.map((nodeId) =>
            this.iamApi.removeNodeList({
                list: listId,
                person: userId,
                repository,
                node: nodeId,
            }),
        );
        // TODO: replace `concat` / `toArray` with `forkJoin` when the backend can handle
        // simultaneous requests.
        return rxjs.concat(...observables).pipe(
            toArray(), // Replace until here.
            // return rxjs.forkJoin(observables).pipe(
            map(() => void 0),
            finalize(() => this.nodeListChangesSubject.next(listId)),
        );
    }

    /**
     * Creates a shared observable for `getNodeList` for the given params.
     */
    private createGetNodeList([listId, options]: Parameters<NodeListService['observeNodeList']>) {
        const { userId, repository, propertyFilter, sortPolicies } = {
            ...new GetNodeListOptions(),
            ...options,
        };
        const getNodeList$ = this.iamApi.getNodeList({
            list: listId,
            person: userId,
            repository,
            propertyFilter,
            ...mapSortPolicies(sortPolicies),
        });
        const nodeListNeedsRefresh$ = rxjs.merge(
            this.authentication.observeUserChanges(),
            this.nodeListChangesSubject.pipe(filter((id) => id === listId)),
        );
        return nodeListNeedsRefresh$.pipe(
            startWith(void 0 as void),
            switchReplay(() => getNodeList$),
        );
    }
}

function mapSortPolicies(sortPolicies?: SortPolicy[]): {
    sortProperties?: string[];
    sortAscending?: boolean[];
} {
    if (!sortPolicies) {
        return {};
    }
    const result = {
        sortProperties: [] as string[],
        sortAscending: [] as boolean[],
    };
    for (const sortPolicy of sortPolicies) {
        result.sortProperties.push(sortPolicy.property);
        result.sortAscending.push(sortPolicy.direction === 'asc');
    }
    return result;
}
