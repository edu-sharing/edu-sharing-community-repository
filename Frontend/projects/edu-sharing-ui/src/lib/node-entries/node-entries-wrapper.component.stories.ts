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
    AuthenticationService,
    EduSharingApiModule,
    Node,
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
    DummyNode,
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
            console.log(n);
            return n;
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
            providers: [
                provideAnimations(),
                {
                    provide: ApiRequestConfiguration,
                    useValue: {},
                },
                {
                    provide: AuthenticationService,
                    useClass: AuthenticationServiceMock,
                },
                {
                    provide: NodeEntriesService,
                    useClass: NodeEntriesServiceMock,
                },
                {
                    provide: TranslateService,
                    useClass: translateProvider,
                },
                {
                    provide: Toast,
                    useValue: ToastMock,
                },
            ],
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
        dataSource: loadingDataSource,
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
