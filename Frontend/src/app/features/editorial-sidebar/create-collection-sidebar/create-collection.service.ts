import { Injectable, inject } from '@angular/core';
import {
    CollectionServiceUnwrapped,
    HOME_REPOSITORY,
    Node,
    NodeService,
    NodeServiceUnwrapped,
    ROOT,
    SessionStorageService,
    Store,
} from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { RestConstants } from '../../../core-module/core.module';

/** the kinds of collection that can be created */
export type NewCollectionType = 'default' | 'EDITORIAL' | 'MEDIA_CENTER';

/** how the children of a collection are sorted by default */
export interface NewCollectionSort {
    /** the property the children are sorted by */
    property: string;
    ascending: boolean;
}

export interface NewCollection {
    /** the collection the new one is created in, `null` for the top level */
    parent: Node | null;
    /** title, description, colour and author of the new collection */
    draft: Node;
    type: NewCollectionType;
    /** values of the metadata group of the chosen type */
    metadata: { [property: string]: string[] };
    subcollectionSort: NewCollectionSort;
    referenceSort: NewCollectionSort;
    /** the image of the collection, if one was chosen */
    image: File | null;
}

export interface CollectionChanges {
    /** the collection with its edited title, description, colour and author */
    collection: Node;
    metadata: { [property: string]: string[] };
    /** the new default sorting, unset to leave it as it is */
    subcollectionSort: NewCollectionSort | null;
    referenceSort: NewCollectionSort | null;
    /** the new image of the collection, if one was chosen */
    image: File | null;
    removeImage: boolean;
}

/**
 * Creates a collection together with everything the REST call for the collection itself does not
 * cover: its metadata, its default sorting and its image.
 */
@Injectable({ providedIn: 'root' })
export class CreateCollectionService {
    private readonly collectionApi = inject(CollectionServiceUnwrapped);
    private readonly nodeApi = inject(NodeService);
    private readonly nodeApiUnwrapped = inject(NodeServiceUnwrapped);
    private readonly sessionStorage = inject(SessionStorageService);

    async create(request: NewCollection): Promise<Node> {
        const parentId = request.parent?.ref.id ?? ROOT;
        const created = (
            await firstValueFrom(
                this.collectionApi.createCollection({
                    repository: HOME_REPOSITORY,
                    collection: parentId,
                    body: this.getBody(request),
                }),
            )
        ).collection;
        const id = created.ref.id;
        if (Object.keys(request.metadata).length) {
            await this.addMetadataAspects(id);
            await firstValueFrom(this.nodeApi.editNodeMetadata(id, request.metadata));
        }
        await this.setSort(
            id,
            RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE,
            request.subcollectionSort,
        );
        await this.setSort(id, RestConstants.CCM_PROP_COLLECTION_ORDER_MODE, request.referenceSort);
        if (this.sharesWithParent(request.parent)) {
            await firstValueFrom(
                this.nodeApiUnwrapped.setPermission({
                    repository: HOME_REPOSITORY,
                    node: id,
                    sendMail: false,
                    sendCopy: false,
                    mailtext: '',
                    body: { inherited: true, permissions: [] },
                }),
            );
        }
        if (request.image) {
            await this.setImage(id, request.image);
        }
        if (parentId === ROOT) {
            await this.rememberRootCollection(created);
        }
        return created;
    }

    /** Stores the edited collection together with its metadata, default sorting and image. */
    async update(changes: CollectionChanges): Promise<void> {
        const node = changes.collection;
        const id = node.ref.id;
        await firstValueFrom(
            this.collectionApi.updateCollection({
                repository: HOME_REPOSITORY,
                collection: id,
                body: {
                    ...node,
                    title: node.title.trim(),
                    // the rating belongs to the node, not to its collection data
                    rating: null,
                    collection: {
                        ...node.collection,
                        description: (node.collection.description ?? '').trim(),
                    },
                } as Node,
            }),
        );
        if (Object.keys(changes.metadata).length) {
            await this.addMetadataAspects(id);
            await firstValueFrom(this.nodeApi.editNodeMetadata(id, changes.metadata));
        }
        if (changes.subcollectionSort) {
            await this.setSort(
                id,
                RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE,
                changes.subcollectionSort,
            );
        }
        if (changes.referenceSort) {
            await this.setSort(
                id,
                RestConstants.CCM_PROP_COLLECTION_ORDER_MODE,
                changes.referenceSort,
            );
        }
        if (changes.image) {
            await this.setImage(id, changes.image);
        } else if (changes.removeImage) {
            await firstValueFrom(
                this.collectionApi.removeIconOfCollection({
                    repository: HOME_REPOSITORY,
                    collection: id,
                }),
            );
        }
    }

    /**
     * The search index lags behind, so the top level list adds collections created within the last
     * minute from here.
     */
    private async rememberRootCollection(collection: Node): Promise<void> {
        const collections = await this.sessionStorage.get(
            SessionStorageService.KEY_ROOT_COLLECTIONS,
            [],
            Store.BrowserSessionStorage,
        );
        collections.push(collection);
        await this.sessionStorage.set(
            SessionStorageService.KEY_ROOT_COLLECTIONS,
            collections,
            Store.BrowserSessionStorage,
            60,
        );
    }

    private getBody(request: NewCollection): Node {
        const draft = request.draft;
        return {
            ...draft,
            title: draft.title.trim(),
            collection: {
                ...draft.collection,
                description: (draft.collection.description ?? '').trim(),
                type: request.type,
                scope: this.getScope(request.parent),
            },
        };
    }

    /** a collection is private until it is shared, but a sub collection stays in the scope of its parent */
    private getScope(parent: Node | null): string {
        const scope = parent?.collection?.scope;
        if (!scope) {
            return RestConstants.COLLECTIONSCOPE_MY;
        }
        return scope === RestConstants.COLLECTIONSCOPE_CUSTOM_PUBLIC
            ? RestConstants.COLLECTIONSCOPE_CUSTOM
            : scope;
    }

    private sharesWithParent(parent: Node | null): boolean {
        return !!parent && this.getScope(parent) !== RestConstants.COLLECTIONSCOPE_MY;
    }

    /** the metadata properties belong to these aspects, which a plain collection lacks */
    private addMetadataAspects(id: string): Promise<unknown> {
        return firstValueFrom(
            this.nodeApiUnwrapped.addAspects({
                repository: HOME_REPOSITORY,
                node: id,
                body: [
                    RestConstants.CCM_ASPECT_LOMREPLICATION,
                    RestConstants.CCM_ASPECT_CCLOM_GENERAL,
                ],
            }),
        );
    }

    private setImage(id: string, image: File): Promise<unknown> {
        return firstValueFrom(
            this.collectionApi.changeIconOfCollection({
                repository: HOME_REPOSITORY,
                collection: id,
                mimetype: 'image/png',
                body: { file: image },
            }),
        );
    }

    private setSort(id: string, property: string, sort: NewCollectionSort): Promise<unknown> {
        return firstValueFrom(
            this.nodeApi.setProperty(HOME_REPOSITORY, id, property, [
                sort.property,
                String(sort.ascending),
            ]),
        );
    }
}
