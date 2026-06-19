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

const dummyDataSource = new NodeDataSource<Node>(
    Array(16)
        .fill(DummyNode)
        .map((n: Node, i) => {
            n = Helper.deepCopy(n);
            n.ref.id = 'id_' + i;
            n.title += ' ' + i;
            return n;
        }),
);
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
