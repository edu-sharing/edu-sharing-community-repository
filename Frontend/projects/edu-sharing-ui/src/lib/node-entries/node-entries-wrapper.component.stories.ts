import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
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
} from 'ngx-edu-sharing-api';

import {
    EduSharingUiCommonModule,
    Helper,
    NodeDataSource,
    NodeEntriesModule,
    NodeEntriesService,
    OptionItem,
    Toast,
} from 'ngx-edu-sharing-ui';
import { NodeEntriesWrapperComponent } from './node-entries-wrapper.component';
import {
    AuthenticationServiceMock,
    NodeEntriesServiceMock,
} from './node-entries-card/node-entries-card.component.stories';
import {
    DefaultColumns,
    DummyAssignment,
    DummyNode,
    mdsStorybookProviders,
    ToastMock,
    translateProvider,
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
        const status: Assignment['status'][] = ['DRAFT', 'INPROGRESS', 'CANCELED', 'FINISHED'];
        n.status = status[Math.floor(Math.random() * status.length)];
        n.permissions = [
            {
                role: 'COORDINATOR',
            } as Permission,
        ];
        if (Math.random() > 0.5) {
            n.endTime = new Date(
                new Date().getTime() + 1000 * 86400 * 7 * Math.random(),
            ).toISOString();
        }
        return n;
    });
const dummyDataSourceAssignments = new NodeDataSource<Assignment>(Helper.deepCopy(Assignments));
const dummyDataSourceAssignmentsSubmission = new NodeDataSource<Assignment>(
    Helper.deepCopy(Assignments).map((a: Assignment) => {
        const status: Submission['submissionStatus'][] = ['NOT_STARTED', 'PENDING', 'FINISHED'];
        a.permissions = [];
        a.submissions = [
            {
                submissionStatus: status[Math.floor(Math.random() * status.length)],
            } as Submission,
        ];
        return a;
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
