import { EventEmitter } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    AssignmentV1Service,
    AuthenticationService,
    CollectionService as ApiCollectionService,
    NetworkService,
    Node,
    NodeListService,
    NodeService,
    ROOT,
} from 'ngx-edu-sharing-api';
import {
    ClipboardObject,
    LocalEventsService,
    NodeEntriesGlobalService,
    NodeHelperService as NodeHelperServiceUi,
    TemporaryStorageService,
} from 'ngx-edu-sharing-ui';
import { of } from 'rxjs';
import {
    ConfigurationService,
    RestCollectionService,
    RestConnectorService,
} from '../core-module/core.module';
import { RestConstants } from '../core-module/rest/rest-constants';
import { RestConnectorsService } from '../core-module/rest/services/rest-connectors.service';
import { RestNodeService } from '../core-module/rest/services/rest-node.service';
import { UIService } from '../core-module/rest/services/ui.service';
import { CollectionNavigationService } from '../features/collection-info/collection-navigation.service';
import { DialogsService } from '../features/dialogs/dialogs.service';
import { EditorialSidebarService } from '../features/editorial-sidebar/editorial-sidebar.service';
import { MainNavService } from '../main/navigation/main-nav.service';
import { GlobalCollectionsPageService } from '../pages/collections-page/global-collections-page.service';
import { BridgeService } from './bridge.service';
import { GlobalOptionsService } from './global-options.service';
import { NodeHelperService } from './node-helper.service';
import { OptionsHelperService } from './options-helper.service';
import { Toast } from './toast';

function collection(id: string, parent: string, type = RestConstants.COLLECTIONTYPE_DEFAULT): Node {
    return {
        ref: { id },
        parent: { id: parent },
        type: RestConstants.CCM_TYPE_MAP,
        aspects: [RestConstants.CCM_ASPECT_COLLECTION],
        collection: { type },
    } as unknown as Node;
}

/** A material listed in a collection: a reference that points to its original. */
function reference(id: string, parent: string): Node {
    return {
        ref: { id },
        parent: { id: parent },
        type: RestConstants.CCM_TYPE_IO,
        aspects: [RestConstants.CCM_ASPECT_IO_REFERENCE],
        properties: { [RestConstants.CCM_PROP_IO_ORIGINAL]: [`original-${id}`] },
    } as unknown as Node;
}

function folder(id: string): Node {
    return { ref: { id }, type: RestConstants.CCM_TYPE_MAP, aspects: [] } as unknown as Node;
}

describe('OptionsHelperService clipboard in collections', () => {
    let service: OptionsHelperService;
    let storage: Map<string, unknown>;
    let hasWriteRight: boolean;
    let copyCollection: jasmine.Spy;
    let addToCollection: jasmine.Spy;
    let moveNode: jasmine.Spy;
    let toastError: jasmine.Spy;

    const source = collection('source', 'parent');
    const target = collection('target', 'parent');
    const subCollection = collection('sub', 'source');
    const material = reference('material', 'source');

    /** Cuts or copies the nodes as selected in the source collection. */
    const putIntoClipboard = (nodes: Node[], copy: boolean) =>
        service.cutCopyNode(
            { scope: null, parent: source, selectedObjects: nodes },
            undefined,
            copy,
        );
    const pasteIntoCollection = (into: Node): Promise<void> =>
        (service as any).pasteIntoCollection(into);

    beforeEach(() => {
        storage = new Map();
        hasWriteRight = true;
        copyCollection = jasmine
            .createSpy('copyCollection')
            .and.callFake(({ sourceCollection }) =>
                of({ root: collection(`copy-${sourceCollection}`, 'target'), entries: [] }),
            );
        addToCollection = jasmine
            .createSpy('addToCollection')
            .and.callFake((_collection, _nodes, _proposal, callback) => callback?.([]));
        moveNode = jasmine
            .createSpy('moveNode')
            .and.callFake((_target: string, id: string) => of({ node: { ref: { id } } }));
        toastError = jasmine.createSpy('error');
        const noop = (): void => undefined;
        TestBed.configureTestingModule({
            providers: [
                OptionsHelperService,
                {
                    provide: TemporaryStorageService,
                    useValue: {
                        get: (key: string) => storage.get(key),
                        set: (key: string, value: unknown) => storage.set(key, value),
                        remove: (key: string) => storage.delete(key),
                    },
                },
                { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
                {
                    provide: NodeHelperService,
                    useValue: {
                        getNodesRight: () => hasWriteRight,
                        getOriginalId: (node: Node) =>
                            node.properties?.[RestConstants.CCM_PROP_IO_ORIGINAL]?.[0] ??
                            node.ref.id,
                    },
                },
                { provide: BridgeService, useValue: { showTemporaryMessage: noop } },
                { provide: ApiCollectionService, useValue: { copyCollection } },
                { provide: RestNodeService, useValue: { moveNode } },
                { provide: UIService, useValue: { addToCollection } },
                {
                    provide: RestConnectorService,
                    useValue: { hasToolPermissionInstant: () => true },
                },
                {
                    provide: Toast,
                    useValue: {
                        error: toastError,
                        toast: noop,
                        showProgressSpinner: noop,
                        closeProgressSpinner: noop,
                    },
                },
                {
                    provide: LocalEventsService,
                    useValue: {
                        nodesCreated: new EventEmitter(),
                        nodesChanged: new EventEmitter(),
                    },
                },
                {
                    provide: GlobalCollectionsPageService,
                    useValue: { addTemporaryCollections: () => Promise.resolve() },
                },
                { provide: TranslateService, useValue: { instant: (key: string) => key } },
                ...[
                    NodeHelperServiceUi,
                    AuthenticationService,
                    NetworkService,
                    RestCollectionService,
                    ConfigurationService,
                    GlobalOptionsService,
                    NodeEntriesGlobalService,
                    RestConnectorsService,
                    DialogsService,
                    MainNavService,
                    EditorialSidebarService,
                    CollectionNavigationService,
                    NodeListService,
                    NodeService,
                    Router,
                    AssignmentV1Service,
                ].map((token) => ({ provide: token, useValue: {} })),
            ],
        });
        service = TestBed.inject(OptionsHelperService);
    });

    describe('copy', () => {
        it('allows pasting collections, materials and a mix of both into a collection', () => {
            for (const nodes of [[subCollection], [material], [subCollection, material]]) {
                putIntoClipboard(nodes, true);
                expect(service.canPasteIntoCollection(target))
                    .withContext(`${nodes.length}`)
                    .toBe(true);
            }
        });

        it('copies the collections and adds the materials as references for a mixed clipboard', async () => {
            putIntoClipboard([subCollection, material], true);
            await pasteIntoCollection(target);
            expect(copyCollection).toHaveBeenCalledOnceWith(
                jasmine.objectContaining({ sourceCollection: 'sub', targetCollection: 'target' }),
            );
            expect(addToCollection).toHaveBeenCalledTimes(1);
            const [into, nodes] = addToCollection.calls.mostRecent().args;
            expect(into).toBe(target);
            expect(nodes.map((node: Node) => node.ref.id)).toEqual(['original-material']);
            expect(storage.size).toBe(0);
        });

        it('only copies the collections of a clipboard without materials', async () => {
            putIntoClipboard([subCollection], true);
            await pasteIntoCollection(target);
            expect(copyCollection).toHaveBeenCalledTimes(1);
            expect(addToCollection).not.toHaveBeenCalled();
        });

        it('only adds the materials of a clipboard without collections', async () => {
            putIntoClipboard([material], true);
            await pasteIntoCollection(target);
            expect(copyCollection).not.toHaveBeenCalled();
            expect(addToCollection).toHaveBeenCalledTimes(1);
        });

        it('pastes only collections onto the start page', () => {
            const root = { ref: { id: ROOT }, aspects: [RestConstants.CCM_ASPECT_COLLECTION] };
            putIntoClipboard([subCollection], true);
            expect(service.canPasteIntoCollection(root as unknown as Node)).toBe(true);
            putIntoClipboard([subCollection, material], true);
            expect(service.canPasteIntoCollection(root as unknown as Node)).toBe(false);
        });

        it('refuses a collection without write permission', () => {
            hasWriteRight = false;
            putIntoClipboard([subCollection, material], true);
            expect(service.canPasteIntoCollection(target)).toBe(false);
        });

        it('pastes no clipboard holding a collection into a workspace folder', () => {
            putIntoClipboard([subCollection, material], true);
            expect(service.canPasteIntoFolder(folder('folder'))).toBe(false);
            putIntoClipboard([subCollection], true);
            expect(service.canPasteIntoFolder(folder('folder'))).toBe(false);
        });
    });

    describe('cut', () => {
        it('allows moving collections, materials and a mix of both into another collection', () => {
            for (const nodes of [[subCollection], [material], [subCollection, material]]) {
                putIntoClipboard(nodes, false);
                expect(service.canPasteIntoCollection(target))
                    .withContext(`${nodes.length}`)
                    .toBe(true);
            }
        });

        it('refuses the collection the nodes were cut from and a collection into itself', () => {
            putIntoClipboard([subCollection, material], false);
            expect(service.canPasteIntoCollection(source)).toBe(false);
            expect(service.canPasteIntoCollection(subCollection)).toBe(false);
        });

        it('moves every node of a mixed clipboard and empties it', async () => {
            putIntoClipboard([subCollection, material], false);
            await pasteIntoCollection(target);
            expect(moveNode.calls.allArgs()).toEqual([
                ['target', 'sub'],
                ['target', 'material'],
            ]);
            expect(storage.size).toBe(0);
        });

        it('refuses to move a collection between editorial and other collections', async () => {
            const editorial = collection(
                'editorial',
                'parent',
                RestConstants.COLLECTIONTYPE_EDITORIAL,
            );
            putIntoClipboard([subCollection, material], false);
            await pasteIntoCollection(editorial);
            expect(moveNode).not.toHaveBeenCalled();
            expect(toastError).toHaveBeenCalledWith(null, 'INVALID_OPERATION');
        });

        it('moves materials alone into an editorial collection', async () => {
            const editorial = collection(
                'editorial',
                'parent',
                RestConstants.COLLECTIONTYPE_EDITORIAL,
            );
            putIntoClipboard([material], false);
            await pasteIntoCollection(editorial);
            expect(moveNode).toHaveBeenCalledOnceWith('editorial', 'material');
        });

        it('pastes no nodes cut inside a collection into a workspace folder', () => {
            putIntoClipboard([material], false);
            expect(service.canPasteIntoFolder(folder('folder'))).toBe(false);
        });
    });
});
