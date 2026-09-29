import { SelectionModel } from '@angular/cdk/collections';
import { EventEmitter, signal } from '@angular/core';
import {
    applicationConfig,
    componentWrapperDecorator,
    type Meta,
    type StoryObj,
} from '@storybook/angular';
import {
    AuthenticationService,
    CollectionService as ApiCollectionService,
    NetworkService,
    Node,
    Repository,
    SearchService,
} from 'ngx-edu-sharing-api';
import { Helper, LocalEventsService, NodeEntriesDataType } from 'ngx-edu-sharing-ui';
import { NEVER, Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { expect, userEvent, waitFor } from 'storybook/test';
import { RestConstants } from '../../../core-module/core.module';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';
import { RestNodeService } from '../../../core-module/rest/services/rest-node.service';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { EditorialSidebarService } from '../../../features/editorial-sidebar/editorial-sidebar.service';
import {
    createNetworkServiceMock,
    DialogsServiceMock,
    DummyNode,
    DummyRepository,
    mdsStorybookProviders,
} from '../../../features/mds/mds-editor/storybook-utils';
import { BridgeService } from '../../../services/bridge.service';
import { ConnectorOptionsService } from '../../../services/connector-options.service';
import { LtiToolOptionsService } from '../../../services/lti-tool-options.service';
import { NodeHelperService } from '../../../services/node-helper.service';
import { UploadDialogService } from '../../../services/upload-dialog.service';
import { NodesSelectorComponent, NodesSelectorConfig, TabType } from './nodes-selector.component';

// --- fixtures ----------------------------------------------------------------------------------

const homeRepository = DummyRepository;
const remoteRepositories: Repository[] = [
    { id: 'remote-ddb', title: 'Deutsche Digitale Bibliothek', isHomeRepo: false },
    { id: 'remote-pixabay', title: 'Pixabay', isHomeRepo: false },
];
const allRepositories = [homeRepository, ...remoteRepositories];

/**
 * Search results. The id of the requested repository is part of every node, so the checks can
 * assert which repository was searched without relying on any translated text.
 */
function searchResultNodes(count: number, repo = homeRepository.id): Node[] {
    return Array(count)
        .fill(DummyNode)
        .map((template: Node, index) => {
            const node = Helper.deepCopy(template) as Node;
            node.ref = { ...node.ref, id: `${repo}-node-${index}`, repo };
            node.name = `${repo}-result-${index}`;
            node.title = `${repo}-result-${index}`;
            return node;
        });
}

// --- mocks -------------------------------------------------------------------------------------

type SearchBehavior = 'results' | 'empty' | 'loading';

function searchServiceMock(behavior: SearchBehavior) {
    return {
        search: (request: { repository?: string }): Observable<unknown> => {
            if (behavior === 'loading') {
                return NEVER;
            }
            const nodes =
                behavior === 'empty' ? [] : searchResultNodes(8, request?.repository ?? '');
            // a short delay so the loading state is reachable
            return of({
                nodes,
                facets: [] as unknown[],
                pagination: { from: 0, count: nodes.length, total: nodes.length },
            }).pipe(delay(300));
        },
    };
}

const nodeHelperServiceMock = {
    getDefaultInboxFolder: () => of(DummyNode as Node),
    getSortByForCollection: () => ({ active: RestConstants.CM_MODIFIED_DATE, direction: 'desc' }),
    isNodeCollection: (node: Node) => node?.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION),
    getNodesRight: () => true,
    getOriginalId: (node: Node) => node?.ref?.id,
    handleNodeError: () => 0,
    propertiesFromConnector: () => ({}),
    // a repository icon that does not require a backend
    getSourceIconRepoPath: () =>
        'data:image/svg+xml;base64,' +
        btoa(
            '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16">' +
                '<circle cx="8" cy="8" r="7" fill="#48708e"/></svg>',
        ),
};

const editorialSidebarServiceMock = {
    nodes: signal<NodeEntriesDataType[]>(null),
    sidebarOpened: signal(false),
    sidebarLoading: signal(false),
    applyNodeEmitted: new EventEmitter<{ nodes: Node[] }>(),
    selectNode: (): void => {},
    close: (): void => {},
};

/** the tabs behind a toolpermission (methodology, workspace) are offered in the stories */
const authenticationServiceMock = {
    reportOutsideApiRequest: (): void => {},
    hasToolpermission: () => Promise.resolve(true),
    observeUserChanges: () => of(),
    observeLoginInfo: () =>
        of({
            isValidLogin: true,
            authorityName: 'sample-authority',
            toolPermissions: [
                RestConstants.TOOLPERMISSION_WORKSPACE,
                RestConstants.TOOLPERMISSION_METADATA_METHODOLOGY,
            ],
            statusCode: RestConstants.STATUS_CODE_OK,
            isAdmin: false,
            isGuest: false,
            sessionTimeout: 3600,
        }),
};

/** providers shared by all stories, the repository / search setup is added per story */
const baseProviders = mdsStorybookProviders.concat([
    { provide: AuthenticationService, useValue: authenticationServiceMock },
    { provide: NodeHelperService, useValue: nodeHelperServiceMock },
    { provide: RestCollectionService, useValue: { getCollectionSubcollections: () => of({}) } },
    { provide: ApiCollectionService, useValue: {} },
    { provide: RestNodeService, useValue: {} },
    { provide: EditorialSidebarService, useValue: editorialSidebarServiceMock },
    { provide: ConnectorOptionsService, useValue: { buildOptions: () => of([]) } },
    {
        provide: LtiToolOptionsService,
        useValue: { buildOptions: () => of([]), createFromDialogResult: () => Promise.resolve([]) },
    },
    { provide: UIService, useValue: {} },
    { provide: UploadDialogService, useValue: {} },
    { provide: DialogsService, useClass: DialogsServiceMock },
    { provide: BridgeService, useValue: { showTemporaryMessage: (): void => {} } },
    LocalEventsService,
]);

/** repository + search setup of a single story */
function setup(repositories: Repository[], behavior: SearchBehavior = 'results') {
    return applicationConfig({
        providers: [
            { provide: NetworkService, useValue: createNetworkServiceMock(repositories) },
            { provide: SearchService, useValue: searchServiceMock(behavior) },
        ],
    });
}

/** an option state as the editorial sidebar passes it in */
function option(optionConfig: NodesSelectorConfig) {
    return { option: 'SORT_INTO', trap: false, optionConfig };
}

/** a selection that makes the component act as a target for the given nodes */
function selection(nodes: Node[]): SelectionModel<NodeEntriesDataType> {
    return new SelectionModel<NodeEntriesDataType>(true, nodes);
}

// --- check helpers -----------------------------------------------------------------------------
// the checks address elements by their `data-test` marker, never by a (translated) caption

const query = (canvasElement: HTMLElement, marker: string): HTMLElement =>
    canvasElement.querySelector(`[data-test="${marker}"]`);

const visibleTabs = (canvasElement: HTMLElement): string[] =>
    Array.from(canvasElement.querySelectorAll('[data-test^="nodes-selector-tab-label-"]')).map(
        (element) => element.getAttribute('data-test').replace('nodes-selector-tab-label-', ''),
    );

const repositorySelect = (canvasElement: HTMLElement): HTMLElement =>
    query(canvasElement, 'nodes-selector-repository-select');

const openRepositorySelect = (canvasElement: HTMLElement): Promise<void> =>
    userEvent.click(repositorySelect(canvasElement).querySelector('.mat-mdc-select-trigger'));

/** an option of the opened dropdown — its panel lives in the cdk overlay, not in the canvas */
const repositoryOption = (repository: Repository): Promise<HTMLElement> =>
    waitFor(() => {
        const element = query(document.body, `nodes-selector-repository-option-${repository.id}`);
        expect(element).toBeTruthy();
        return element;
    });

/** picks a repository from the dropdown */
async function selectRepository(canvasElement: HTMLElement, repository: Repository): Promise<void> {
    await openRepositorySelect(canvasElement);
    await userEvent.click(await repositoryOption(repository));
}

/** the results of the given repository are listed (every node title carries the repository id) */
const expectResultsOf = (canvasElement: HTMLElement, repositoryId: string) =>
    waitFor(() => expect(canvasElement.textContent).toContain(`${repositoryId}-result-0`), {
        // has to outlast the simulated backend latency
        timeout: 5000,
    });

// --- stories -----------------------------------------------------------------------------------

const meta: Meta<NodesSelectorComponent> = {
    title: 'Editorial/Nodes Selector',
    component: NodesSelectorComponent,
    decorators: [
        // the component fills its host (`:host { height: 100% }`), so the story has to give it one.
        // The width matches the editorial sidebar it usually lives in.
        componentWrapperDecorator(
            (story) =>
                `<div style="height: 100vh; width: 520px; max-width: 100%; display: flex; flex-direction: column">${story}</div>`,
        ),
        applicationConfig({ providers: baseProviders }),
    ],
    args: {
        option: option({ state: TabType.SEARCH }) as any,
        tabBlacklist: [],
    },
    parameters: {
        layout: 'fullscreen',
    },
    tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<NodesSelectorComponent>;

/** a single (home) repository: the search tab offers no repository dropdown at all */
export const SearchSingleRepository: Story = {
    decorators: [setup([homeRepository])],
    play: async ({ canvasElement }) => {
        await expectResultsOf(canvasElement, homeRepository.id);
        expect(repositorySelect(canvasElement)).toBeNull();
    },
};

/** several repositories: each one is offered, the home repository is searched by default */
export const SearchMultipleRepositories: Story = {
    decorators: [setup(allRepositories)],
    play: async ({ canvasElement }) => {
        await expectResultsOf(canvasElement, homeRepository.id);
        await openRepositorySelect(canvasElement);
        for (const repository of allRepositories) {
            expect(await repositoryOption(repository)).toBeTruthy();
        }
        await userEvent.keyboard('{Escape}');
    },
};

/** the selected repository returns nothing */
export const SearchWithoutResults: Story = {
    decorators: [setup(allRepositories, 'empty')],
    play: async ({ canvasElement }) => {
        await waitFor(
            () => expect(query(canvasElement, 'nodes-selector-no-results')).toBeTruthy(),
            {
                timeout: 5000,
            },
        );
        expect(canvasElement.querySelector('es-spinner')).toBeNull();
    },
};

/** the search request never resolves: the spinner stays and no "no results" message flashes */
export const SearchLoading: Story = {
    decorators: [setup(allRepositories, 'loading')],
    play: async ({ canvasElement }) => {
        await waitFor(() => expect(canvasElement.querySelector('es-spinner')).toBeTruthy());
        expect(query(canvasElement, 'nodes-selector-no-results')).toBeNull();
    },
};

/** remote repositories exist but the caller opted out of them */
export const SearchRemoteRepositoriesDisabled: Story = {
    decorators: [setup(allRepositories)],
    args: {
        option: option({ state: TabType.SEARCH, allowRemoteRepositories: false }) as any,
    },
    play: async ({ canvasElement }) => {
        await expectResultsOf(canvasElement, homeRepository.id);
        expect(repositorySelect(canvasElement)).toBeNull();
    },
};

/** only a subset of the tabs is offered */
export const SearchWithRestrictedTabs: Story = {
    decorators: [setup(allRepositories)],
    args: {
        tabBlacklist: [TabType.UPLOAD, TabType.COLLECTIONS],
    },
    play: async ({ canvasElement }) => {
        await waitFor(() => expect(visibleTabs(canvasElement).length).toBeGreaterThan(0));
        expect(visibleTabs(canvasElement)).toEqual([TabType.SEARCH, TabType.WORKSPACE]);
    },
};

/** acting as a target for home repository nodes: no repository dropdown, reduced tabs */
export const TargetModeHomeSelection: Story = {
    decorators: [setup(allRepositories)],
    args: {
        option: option({
            state: TabType.COLLECTIONS,
            selection: selection(searchResultNodes(2)),
        }) as any,
    },
    play: async ({ canvasElement }) => {
        await waitFor(() => expect(visibleTabs(canvasElement).length).toBeGreaterThan(0));
        expect(visibleTabs(canvasElement)).toEqual([
            TabType.METHODOLOGY,
            TabType.COLLECTIONS,
            TabType.WORKSPACE,
        ]);
        expect(repositorySelect(canvasElement)).toBeNull();
    },
};

/** a selected node of a remote repository can only be sorted into a collection */
export const TargetModeRemoteSelection: Story = {
    decorators: [setup(allRepositories)],
    args: {
        option: option({
            state: TabType.COLLECTIONS,
            selection: selection(searchResultNodes(1, remoteRepositories[0].id)),
        }) as any,
    },
    play: async ({ canvasElement }) => {
        await waitFor(() => expect(visibleTabs(canvasElement).length).toBeGreaterThan(0));
        expect(visibleTabs(canvasElement)).toEqual([TabType.COLLECTIONS]);
    },
};

export const CollectionsTab: Story = {
    decorators: [setup(allRepositories)],
    args: {
        option: option({ state: TabType.COLLECTIONS }) as any,
    },
    play: async ({ canvasElement }) => {
        await waitFor(() => expect(visibleTabs(canvasElement)).toContain(TabType.COLLECTIONS));
        // the repository dropdown belongs to the search tab only
        expect(repositorySelect(canvasElement)).toBeNull();
    },
};

export const WorkspaceTab: Story = {
    decorators: [setup(allRepositories)],
    args: {
        option: option({ state: TabType.WORKSPACE }) as any,
    },
};

export const UploadTab: Story = {
    decorators: [setup(allRepositories)],
    args: {
        option: option({ state: TabType.UPLOAD }) as any,
    },
};
