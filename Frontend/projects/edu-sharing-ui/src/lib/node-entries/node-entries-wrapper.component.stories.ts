import { provideAnimations } from '@angular/platform-browser/animations';
import {
    applicationConfig,
    argsToTemplate,
    type Meta,
    moduleMetadata,
    type StoryObj,
} from '@storybook/angular';
import {
    ApiRequestConfiguration,
    Assignment,
    AuthenticationService,
    EduSharingApiModule,
    Node,
    Permission,
    Submission,
    UserSimple,
} from 'ngx-edu-sharing-api';

import {
    ColumnType,
    EduSharingUiCommonModule,
    Helper,
    ListItem,
    NodeDataSource,
    NodeEntriesModule,
    OptionItem,
    SubmissionWithAssignment,
} from 'ngx-edu-sharing-ui';
import { NodeEntriesWrapperComponent } from './node-entries-wrapper.component';
import { AuthenticationServiceMock } from './node-entries-card/node-entries-card.component.stories';
import {
    DefaultColumns,
    DummyAssignment,
    DummyNode,
    DummyUser,
    mdsStorybookProviders,
} from 'src/app/features/mds/mds-editor/storybook-utils';
import { InteractionType, NodeEntriesDisplayType } from './entries-model';
import { expect, userEvent, waitFor, within } from 'storybook/test';

/** `count` distinct copies of `DummyNode`, numbered starting at `offset`. */
const makeNodes = (count: number, offset = 0): Node[] =>
    Array(count)
        .fill(DummyNode)
        .map((n: Node, i) => {
            n = Helper.deepCopy(n);
            n.ref.id = 'id_' + (offset + i);
            n.title += ' ' + (offset + i);
            return n;
        });

const dummyDataSource = new NodeDataSource<Node>(makeNodes(16));
const Assignments = Array(16)
    .fill(DummyAssignment)
    .map((n: Assignment, i) => {
        n = Helper.deepCopy(n);
        n.ref.id = 'id_' + i;
        n.title += ' ' + i;
        if (i === 0) {
            n.status = 'DRAFT';
            n.submissions = [
                {
                    submissionStatus: 'NOT_STARTED',
                    validationStatus: 'NOT_STARTED',
                } as Submission,
            ];
        } else if (i === 1) {
            n.status = 'INPROGRESS';
            n.submissions = [
                {
                    submissionStatus: 'NOT_STARTED',
                    validationStatus: 'NOT_STARTED',
                } as Submission,
            ];
        } else if (i === 2) {
            n.status = 'INPROGRESS';
            n.submissions = [
                {
                    submissionStatus: 'FINISHED',
                    validationStatus: 'NOT_STARTED',
                } as Submission,
            ];
        } else if (i === 3) {
            n.status = 'CORRECTED';
            n.submissions = [
                {
                    submissionStatus: 'FINISHED',
                    validationStatus: 'FINISHED',
                } as Submission,
            ];
        } else if (i === 4) {
            n.status = 'FINISHED';
            n.submissions = [
                {
                    submissionStatus: 'FINISHED',
                    validationStatus: 'FINISHED',
                } as Submission,
            ];
        } else {
            const status: Assignment['status'][] = ['DRAFT', 'INPROGRESS', 'CANCELED', 'FINISHED'];
            n.status = status[Math.floor(Math.random() * status.length)];
            const submissionStatus: Submission['submissionStatus'][] = [
                'NOT_STARTED',
                'PENDING',
                'FINISHED',
            ];
            n.submissions = [
                {
                    submissionStatus:
                        submissionStatus[Math.floor(Math.random() * submissionStatus.length)],
                    validationStatus:
                        submissionStatus[Math.floor(Math.random() * submissionStatus.length)],
                } as Submission,
            ];
        }
        n.submissions = n.submissions.map((s) => {
            return {
                ...s,
                assignee: new DummyUser(),
            };
        });
        n.permissions = [
            {
                role: 'COORDINATOR',
            } as Permission,
            {
                role: 'ASSIGNEE',
            } as Permission,
        ];
        if (Math.random() > 0.5) {
            n.endTime = new Date(
                new Date().getTime() + 1000 * 86400 * 7 * Math.random() - 1000 * 86400 * 3,
            ).toISOString();
        }
        return n;
    });
const dummyDataSourceAssignments = new NodeDataSource<Assignment>(Helper.deepCopy(Assignments));
const dummyDataSourceAssignmentsSubmission = new NodeDataSource<Assignment>(
    Helper.deepCopy(Assignments).map((a: Assignment) => {
        a.permissions = a.permissions.filter((a) => a.role !== 'COORDINATOR');
        return a;
    }),
);
const dummyDataSourceSubmission = new NodeDataSource<SubmissionWithAssignment>(
    Helper.deepCopy(Assignments).map((a: Assignment) => {
        return {
            ...a.submissions[0],
            assignment: a,
        } as SubmissionWithAssignment;
    }),
);
const LOAD_MORE_PAGE_SIZE = 8;
const LOAD_MORE_TOTAL = 24;
/** Simulated backend latency for loading a further page. */
const LOAD_MORE_DELAY = 2000;

/**
 * Reports more available items (`pagination.total`) than it currently holds, so
 * `NodeDataSource.hasMore()` is `true` and the list offers to load the next page.
 *
 * The instance is stable (story `args` reference it) and reset per story run.
 */
const paginatedDataSource = new NodeDataSource<Node>();

function resetPaginatedDataSource(): void {
    paginatedDataSource.isLoading = false;
    paginatedDataSource.setData(makeNodes(LOAD_MORE_PAGE_SIZE), {
        from: 0,
        count: LOAD_MORE_PAGE_SIZE,
        total: LOAD_MORE_TOTAL,
    });
}

/**
 * Appends the next page, standing in for a backend answering the `fetchData` output.
 *
 * Answers only after `LOAD_MORE_DELAY`, with `isLoading: 'page'` in between, so the loading
 * state of the list (spinner, hidden "load more" button) is actually observable.
 */
async function appendNextPage(): Promise<void> {
    // infinite scroll can fire again while a page is still in flight
    if (paginatedDataSource.isLoading) {
        return;
    }
    const loaded = paginatedDataSource.getData().length;
    const count = Math.min(LOAD_MORE_PAGE_SIZE, LOAD_MORE_TOTAL - loaded);
    if (count <= 0) {
        return;
    }
    paginatedDataSource.isLoading = 'page';
    await new Promise((resolve) => setTimeout(resolve, LOAD_MORE_DELAY));
    paginatedDataSource.appendData(makeNodes(count, loaded));
    // `appendData` does not maintain the pagination, so keep it in sync explicitly
    paginatedDataSource.setPagination({
        from: 0,
        count: loaded + count,
        total: LOAD_MORE_TOTAL,
    });
    paginatedDataSource.isLoading = false;
}

/** Holds all available items, so nothing can be loaded any more. */
const fullyLoadedDataSource = new NodeDataSource<Node>();
fullyLoadedDataSource.setData(makeNodes(LOAD_MORE_PAGE_SIZE), {
    from: 0,
    count: LOAD_MORE_PAGE_SIZE,
    total: LOAD_MORE_PAGE_SIZE,
});

const emptyDataSource = new NodeDataSource<Node>([]);
const loadingDataSource = new NodeDataSource<Node>([]);
loadingDataSource.isLoading = true;
const entries: Meta<NodeEntriesWrapperComponent<any>> = {
    title: 'Core UI/Lists/NodeEntriesCard',
    component: NodeEntriesWrapperComponent,
    decorators: [
        moduleMetadata({
            imports: [EduSharingApiModule, NodeEntriesModule, EduSharingUiCommonModule],
            declarations: [],
        }),
        applicationConfig({
            providers: mdsStorybookProviders.concat([
                provideAnimations(),
                {
                    provide: ApiRequestConfiguration,
                    useValue: {},
                },
                {
                    provide: AuthenticationService,
                    useClass: AuthenticationServiceMock,
                },
            ]),
        }),
    ],
    args: {
        displayType: NodeEntriesDisplayType.Grid,
        elementInteractionType: InteractionType.Emitter,
        dataSource: dummyDataSource as any,
        columns: DefaultColumns,
        checkbox: true,
    },
    render: ({ ...args }) => ({
        props: { ...args },
        template: `
<es-node-entries-wrapper ${argsToTemplate(args)}>
    <ng-template #empty>This is the empty template: lorem ipsum dolor sit amet lorem ipsum dolor sit amet lorem ipsum dolor sit amet</ng-template>
</es-node-entries-wrapper>
`,
    }),
    argTypes: {},
    tags: ['autodocs'],
};

export default entries;
type Story = StoryObj<NodeEntriesWrapperComponent<any>>;
export const EntriesSimpleGrid: Story = {
    args: {},
};
export const EntriesTableLoading: Story = {
    args: {
        displayType: NodeEntriesDisplayType.Table,
        dataSource: loadingDataSource as any,
    },
};
export const EntriesHorizontalGrid: Story = {
    args: {
        gridConfig: {
            layout: 'scroll',
            maxRows: 1,
        },
    },
};
export const EntriesHorizontalGridWithAction: Story = {
    args: {
        globalOptions: [new OptionItem('OPTIONS.ADD', 'add', () => {})],
        gridConfig: {
            layout: 'scroll',
            maxRows: 1,
        },
    },
};
export const EntriesHorizontalGridEmpty: Story = {
    args: {
        dataSource: emptyDataSource as any,
        gridConfig: {
            layout: 'scroll',
            maxRows: 1,
        },
    },
};
const DisabledOption = new OptionItem('OPTIONS.ADD', 'add', () => {});
DisabledOption.isEnabled = false;
export const EntriesHorizontalGridEmptyWithDisabledAction: Story = {
    args: {
        globalOptions: [DisabledOption],
        dataSource: emptyDataSource as any,
        gridConfig: {
            layout: 'scroll',
            maxRows: 1,
        },
    },
};
export const EntriesHorizontalGridEmptyWithAction: Story = {
    args: {
        globalOptions: [new OptionItem('OPTIONS.ADD', 'add', () => {})],
        dataSource: emptyDataSource as any,
        gridConfig: {
            layout: 'scroll',
            maxRows: 1,
        },
    },
};
export const EntriesHorizontalGridLoadingWithAction: Story = {
    args: {
        globalOptions: [new OptionItem('OPTIONS.ADD', 'add', () => {})],
        dataSource: loadingDataSource as any,
        gridConfig: {
            layout: 'scroll',
            maxRows: 1,
        },
    },
};
export const EntriesSmallGrid: Story = {
    args: {
        displayType: NodeEntriesDisplayType.SmallGrid,
    },
};
export const EntriesSmallGridAssignments: Story = {
    args: {
        dataSource: dummyDataSourceAssignments as any,
        displayType: NodeEntriesDisplayType.SmallGrid,
    },
};
export const EntriesSmallGridAssignmentsSubmission: Story = {
    args: {
        dataSource: dummyDataSourceAssignmentsSubmission as any,
        displayType: NodeEntriesDisplayType.SmallGrid,
    },
};
export const SubmittedTasksTable: Story = {
    args: {
        dataSource: dummyDataSourceSubmission as any,
        columns: {
            Default: [
                new ListItem('SUBMISSION', 'assignee'),
                new ListItem('SUBMISSION', 'validationStatus'),
            ],
        } as ColumnType,
        displayType: NodeEntriesDisplayType.Table,
    },
};

/**
 * Renders the list with the `fetchData` output wired to a fake backend, which is what enables
 * loading further pages.
 */
const loadMoreRender: Story['render'] = (args) => ({
    props: { ...args, fetchData: () => appendNextPage() },
    template: `
<es-node-entries-wrapper ${argsToTemplate(args)} (fetchData)="fetchData($event)">
</es-node-entries-wrapper>
`,
});

const loadMoreArgs = {
    dataSource: paginatedDataSource as any,
    // note: deliberately no `maxRows` (unlike the stories above) -- a row limit caps the visible
    // items and suppresses loading regardless of the layout
    gridConfig: { layout: 'scroll' as const },
};

const queryLoadMoreButton = (canvasElement: HTMLElement) =>
    canvasElement.querySelector<HTMLElement>('[data-test="load-more"]');

const expectItemCount = async (canvas: ReturnType<typeof within>, count: number) =>
    waitFor(() => expect(canvas.getAllByRole('listitem')).toHaveLength(count), {
        // has to outlast the simulated backend latency
        timeout: LOAD_MORE_DELAY + 3000,
    });

/** Scrolling the horizontal strip towards its end loads the next page automatically. */
export const EntriesHorizontalGridScrollInfiniteScroll: Story = {
    args: loadMoreArgs,
    render: loadMoreRender,
    beforeEach: () => resetPaginatedDataSource(),
    play: async ({ canvasElement }) => {
        const canvas = within(canvasElement);
        await expectItemCount(canvas, LOAD_MORE_PAGE_SIZE);
        const strip = canvasElement.querySelector<HTMLElement>('.card-grid-layout-scroll');
        expect(strip).not.toBeNull();
        expect(strip.scrollWidth).toBeGreaterThan(strip.clientWidth);
        // `esInfiniteScroll` listens on the element itself, outside the Angular zone
        strip.scrollLeft = strip.scrollWidth;
        strip.dispatchEvent(new Event('scroll'));
        await expectItemCount(canvas, 2 * LOAD_MORE_PAGE_SIZE);
    },
};

/** Nothing left to load: no button, and scrolling must not request another page. */
export const EntriesHorizontalGridScrollFullyLoaded: Story = {
    args: {
        dataSource: fullyLoadedDataSource as any,
        gridConfig: { layout: 'scroll' },
    },
    render: loadMoreRender,
    play: async ({ canvasElement }) => {
        const canvas = within(canvasElement);
        await expectItemCount(canvas, LOAD_MORE_PAGE_SIZE);
        expect(queryLoadMoreButton(canvasElement)).toBeNull();
    },
};
