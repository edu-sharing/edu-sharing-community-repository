import { computed, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { Ace, Node, NodeService, NodeStats, RestConstants } from 'ngx-edu-sharing-api';

/**
 * Details of a single collection that are not part of the collection node itself: the
 * authorities it is shared with, its owner and its view statistics.
 *
 * All of them are only requested for users who may change the permissions of the collection. They
 * are requested once a host calls `activate()` and again whenever the collection id changes.
 * Provide this service on the component that owns the collection so that all components below it
 * share one set of requests.
 */
@Injectable()
export class CollectionDetailsService {
    private readonly nodeService = inject(NodeService);

    /** The collection the details belong to. */
    readonly collection = signal<Node | undefined>(undefined);
    private readonly active = signal(false);

    private readonly collectionId = computed(() => {
        const collection = this.collection();
        const canManage = collection?.access?.includes(RestConstants.ACCESS_CHANGE_PERMISSIONS);
        return this.active() && canManage ? collection.ref.id : undefined;
    });

    private readonly permissionsResource = rxResource({
        params: () => this.collectionId(),
        stream: ({ params }) =>
            this.nodeService
                .getPermissions(params)
                .pipe(
                    map((acl) => acl.localPermissions.permissions.concat(acl.inheritedPermissions)),
                ),
    });
    /** the node with all its properties, the collection of the list only has a part of them */
    private readonly nodeResource = rxResource({
        params: () => this.collectionId(),
        stream: ({ params }) => this.nodeService.getNode(params),
    });
    private readonly statsResource = rxResource({
        params: () => this.collectionId(),
        stream: ({ params }) => this.nodeService.getStats(params),
    });

    /** Local and inherited permissions; `undefined` until loaded or when not permitted. */
    readonly permissions = computed<Ace[] | undefined>(() =>
        this.permissionsResource.hasValue() ? this.permissionsResource.value() : undefined,
    );
    /** The permissions with every authority listed once. */
    readonly uniquePermissions = computed(() =>
        this.permissions()?.filter(
            (p, i, arr) =>
                arr.findIndex((q) => q.authority.authorityName === p.authority.authorityName) === i,
        ),
    );
    /** Authority name of the owner of the collection; `undefined` until loaded or not permitted. */
    readonly ownerName = computed<string | undefined>(() =>
        this.nodeResource.hasValue()
            ? this.nodeResource.value().properties?.['cm:owner']?.[0]
            : undefined,
    );
    readonly stats = computed<NodeStats | undefined>(() =>
        this.statsResource.hasValue() ? this.statsResource.value() : undefined,
    );
    /** Total number of views of the collection. */
    readonly viewCount = computed(() => this.stats()?.total?.['VIEW_COLLECTION']);

    /** Starts requesting the details for the current collection. */
    activate(): void {
        this.active.set(true);
    }
}
