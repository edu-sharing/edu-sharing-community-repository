import { EventEmitter, Injectable } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { applicationConfig, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import {
    ApiRequestConfiguration,
    AuthenticationService,
    EduSharingApiModule,
    NetworkService,
    Node,
    ProposalNode,
} from 'ngx-edu-sharing-api';
import {
    DefaultGroups,
    Helper,
    NodeEntriesDisplayType,
    OptionData,
    OptionItem,
    OptionsHelperComponents,
    OptionsHelperService,
    Target,
} from 'ngx-edu-sharing-ui';
import { NEVER, Observable, of } from 'rxjs';
import { expect, userEvent, waitFor } from 'storybook/test';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';
import {
    createNetworkServiceMock,
    DummyNode,
    DummyRepository,
    mdsStorybookProviders,
    OptionsHelperServiceMock,
} from '../../../features/mds/mds-editor/storybook-utils';
import { MainNavService } from '../../../main/navigation/main-nav.service';
import { SharedModule } from '../../../shared/shared.module';
import { CollectionProposalsComponent } from './collection-proposals.component';

// --- fixtures ----------------------------------------------------------------------------------

const collection = {
    ...Helper.deepCopy(DummyNode),
    ref: { ...DummyNode.ref, id: 'collection' },
    title: 'Collection',
    aspects: [RestConstants.CCM_ASPECT_COLLECTION],
} as Node;

const PROPOSED_AT = '2026-11-26T14:37:00Z';

/** the proposal node itself: who proposed the material and when */
function proposalOf(index: number): Node {
    return {
        ref: { ...DummyNode.ref, id: `proposal-${index}` },
        type: RestConstants.CCM_TYPE_COLLECTION_PROPOSAL,
        name: `proposal-${index}`,
        createdAt: PROPOSED_AT,
        createdBy: { firstName: 'Proposing', lastName: `User ${index}` },
        // the date column reads the creation date from the properties
        properties: {
            [RestConstants.CM_PROP_C_CREATED + '_LONG']: [String(Date.parse(PROPOSED_AT))],
        },
    } as unknown as Node;
}

/** a proposed material the user may read, listed with the proposal it belongs to */
function proposedMaterial(index: number): ProposalNode {
    const node = Helper.deepCopy(DummyNode) as Node;
    return {
        ...node,
        ref: { ...node.ref, id: `proposed-material-${index}` },
        name: `proposed-material-${index}`,
        title: `Proposed material ${index}`,
        proposal: proposalOf(index),
        status: 'PENDING',
        accessible: true,
    } as ProposalNode;
}

/**
 * A proposed material the user may not read: the repository only lists a dummy with the name of
 * the proposal, like `CollectionDao.getCollectionProposals` does.
 */
function unreadableMaterial(index: number): ProposalNode {
    return {
        ref: { ...DummyNode.ref, id: `unreadable-material-${index}` },
        type: 'ccm:io',
        name: `unreadable-material-${index}`,
        // a preview of the material is still linked, it only answers for permitted users
        preview: { url: 'assets/images/storybook/link.svg' },
        icon: { url: 'assets/images/storybook/link.svg' },
        properties: {},
        aspects: [],
        proposal: proposalOf(index),
        status: 'PENDING',
        accessible: false,
    } as unknown as ProposalNode;
}

// --- mocks -------------------------------------------------------------------------------------

type ProposalsBehavior = ProposalNode[] | 'loading';

/** answers the request for the proposals of the collection */
function restCollectionServiceMock(behavior: ProposalsBehavior) {
    return {
        getCollectionProposals: (): Observable<unknown> => {
            if (behavior === 'loading') {
                return NEVER;
            }
            return of({
                nodes: behavior.map((node) => Helper.deepCopy(node)),
                pagination: { from: 0, count: behavior.length, total: behavior.length },
            });
        },
    };
}

/**
 * The lists only ask whether the user is logged in and which toolpermissions they have. The login
 * info has to answer, otherwise the lists wait for the current user before they offer options.
 */
const authenticationServiceMock = {
    hasToolpermission: () => Promise.resolve(true),
    observeLoginInfo: () => of({ isValidLogin: false }),
    observeUserChanges: () => of(),
};

/** the proposals decided on through the buttons of the section, read by the checks */
const decisions: string[] = [];

/**
 * The options of the selected proposals, as the options of the application offer them: accepting
 * them into the collection is the primary action, declining them the second one.
 */
function proposalOptions(proposals: ProposalNode[]): OptionItem[] {
    const decide = (decision: string) => () =>
        decisions.push(...proposals.map((proposal) => `${decision} ${proposal.ref.id}`));
    const accept = new OptionItem('OPTIONS.COLLECTION_PROPOSAL_ACCEPT', 'check', decide('accept'));
    accept.group = DefaultGroups.Primary;
    accept.showAsAction = true;
    accept.priority = 10;
    const decline = new OptionItem(
        'OPTIONS.COLLECTION_PROPOSAL_DECLINE',
        'clear',
        decide('decline'),
    );
    decline.group = DefaultGroups.Primary;
    decline.priority = 20;
    return [accept, decline];
}

/** offers the options of the proposals once proposals are selected */
@Injectable()
class ProposalOptionsHelperServiceMock extends OptionsHelperServiceMock {
    override async getAvailableOptions(
        target: Target,
        objects: Node[],
        components?: OptionsHelperComponents,
        data?: OptionData,
    ): Promise<OptionItem[]> {
        const options = await super.getAvailableOptions(target, objects, components, data);
        const selected = (components?.list?.getSelection()?.selected ?? []) as ProposalNode[];
        if (target !== Target.Actionbar || !selected.length) {
            return options;
        }
        return [...options, ...(await this.filterOptions(proposalOptions(selected), target))];
    }
}

/** the proposals are reloaded on events of the management dialogs, which never fire here */
const mainNavServiceMock = {
    getDialogs: () => ({ eventTriggered: new EventEmitter<unknown>() }),
};

/** the proposals the repository answers with in a single story */
function setup(behavior: ProposalsBehavior) {
    return applicationConfig({
        providers: [
            { provide: RestCollectionService, useValue: restCollectionServiceMock(behavior) },
        ],
    });
}

// --- check helpers -----------------------------------------------------------------------------

const cards = (canvasElement: HTMLElement): HTMLElement[] =>
    Array.from(canvasElement.querySelectorAll('es-node-entries-card'));

const tableRows = (canvasElement: HTMLElement): HTMLElement[] =>
    Array.from(canvasElement.querySelectorAll('mat-row, .mat-mdc-row'));

/** a button of the actionbar of the section, found by the option it offers */
const optionButton = (canvasElement: HTMLElement, option: string): HTMLElement =>
    canvasElement.querySelector(`es-actionbar button.${option.replace(/[:.]/g, '_')}`);

/** what the outputs reported so far, as the story template logs it */
const events = (canvasElement: HTMLElement): string[] =>
    Array.from(canvasElement.querySelectorAll('[data-test="proposal-events"] li')).map((item) =>
        item.textContent.trim(),
    );

/** the proposals are listed once the mocked request answered */
const expectProposals = (canvasElement: HTMLElement, count: number) =>
    waitFor(
        () => expect(cards(canvasElement).length + tableRows(canvasElement).length).toBe(count),
        // has to outlast the simulated latency of the repository and the first render
        { timeout: 5000 },
    );

// --- stories -----------------------------------------------------------------------------------

const meta: Meta<CollectionProposalsComponent> = {
    title: 'Collections/Proposals',
    component: CollectionProposalsComponent,
    decorators: [
        moduleMetadata({
            imports: [SharedModule, EduSharingApiModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders.concat([
                provideAnimations(),
                { provide: ApiRequestConfiguration, useValue: {} },
                { provide: AuthenticationService, useValue: authenticationServiceMock },
                { provide: NetworkService, useValue: createNetworkServiceMock([DummyRepository]) },
                { provide: MainNavService, useValue: mainNavServiceMock },
                { provide: OptionsHelperService, useClass: ProposalOptionsHelperServiceMock },
            ]),
        }),
    ],
    args: {
        collection,
        canEdit: true,
        displayType: NodeEntriesDisplayType.Grid,
    },
    // The outputs are logged below the list. Storybook does not hand functions of the args (like
    // `fn()` spies) to the story template, so the checks read the log instead.
    render: (args) => ({
        props: { ...args, events: [] as string[] },
        template: `
<es-collection-proposals
    [collection]="collection"
    [canEdit]="canEdit"
    [displayType]="displayType"
    (contentClick)="events.push('click ' + $event.element.ref.id)"
    (contentDblClick)="events.push('dblclick ' + $event.element.ref.id)"
    (selectionChange)="events.push('selection ' + $event.length + ' ' + ($event[0]?.ref?.id ?? ''))"
></es-collection-proposals>
<ol data-test="proposal-events">
    @for (event of events; track $index) {
    <li>{{ event }}</li>
    }
</ol>`,
    }),
    tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<CollectionProposalsComponent>;

/**
 * The section as an editor of the collection sees it: once proposals are selected, they are
 * accepted into the collection or declined with the buttons next to the heading.
 */
export const AcceptAndDecline: Story = {
    decorators: [setup([proposedMaterial(1), proposedMaterial(2), unreadableMaterial(3)])],
    play: async ({ canvasElement }) => {
        decisions.length = 0;
        await expectProposals(canvasElement, 3);
        const accept = 'OPTIONS.COLLECTION_PROPOSAL_ACCEPT';
        const decline = 'OPTIONS.COLLECTION_PROPOSAL_DECLINE';
        // nothing to decide on without a selection
        await expect(optionButton(canvasElement, accept)).toBeNull();
        await userEvent.click(cards(canvasElement)[0].querySelector('mat-checkbox input'));
        await userEvent.click(cards(canvasElement)[1].querySelector('mat-checkbox input'));
        await waitFor(() => expect(optionButton(canvasElement, accept)).toBeTruthy());
        await expect(optionButton(canvasElement, decline)).toBeTruthy();
        await userEvent.click(optionButton(canvasElement, accept));
        await expect(decisions).toEqual([
            'accept proposed-material-1',
            'accept proposed-material-2',
        ]);
    },
};

/**
 * The proposals as tiles, as in the tile view of a collection. A click and a double click are
 * reported separately, the host decides what each of them does.
 */
export const Tiles: Story = {
    decorators: [setup([proposedMaterial(1), proposedMaterial(2), proposedMaterial(3)])],
    play: async ({ canvasElement }) => {
        await expectProposals(canvasElement, 3);
        const body = cards(canvasElement)[0].querySelector<HTMLElement>('.card-body');
        await userEvent.click(body);
        await waitFor(() => expect(events(canvasElement)).toEqual(['click proposed-material-1']));
        await userEvent.dblClick(body);
        await waitFor(() =>
            expect(events(canvasElement)).toContain('dblclick proposed-material-1'),
        );
    },
};

/** The proposals as a table, as in the list and the tree view of a collection. */
export const Table: Story = {
    decorators: [setup([proposedMaterial(1), proposedMaterial(2), proposedMaterial(3)])],
    args: { displayType: NodeEntriesDisplayType.Table },
    play: async ({ canvasElement }) => {
        await expectProposals(canvasElement, 3);
    },
};

/** Ticking the checkbox of a proposal reports the selected proposals to the host. */
export const SelectWithCheckbox: Story = {
    decorators: [setup([proposedMaterial(1), proposedMaterial(2)])],
    play: async ({ canvasElement }) => {
        await expectProposals(canvasElement, 2);
        const checkbox = cards(canvasElement)[1].querySelector<HTMLElement>('mat-checkbox input');
        await userEvent.click(checkbox);
        // the checkbox selects without a click on the proposal itself
        await waitFor(() =>
            expect(events(canvasElement)).toEqual(['selection 1 proposed-material-2']),
        );
    },
};

/** A proposed material the user may not read is still listed, with the name of the proposal. */
export const UnreadableMaterial: Story = {
    decorators: [setup([proposedMaterial(1), unreadableMaterial(2)])],
    play: async ({ canvasElement }) => {
        await expectProposals(canvasElement, 2);
    },
};

/** While the proposals load, the section is not shown yet. */
export const Loading: Story = {
    decorators: [setup('loading')],
    play: async ({ canvasElement }) => {
        await expect(canvasElement.querySelector('es-node-entries-wrapper')).toBeNull();
    },
};

/** Without the right to edit the collection the proposals are neither loaded nor shown. */
export const WithoutEditPermission: Story = {
    decorators: [setup([proposedMaterial(1)])],
    args: { canEdit: false },
    play: async ({ canvasElement }) => {
        await expect(canvasElement.querySelector('es-node-entries-wrapper')).toBeNull();
    },
};
