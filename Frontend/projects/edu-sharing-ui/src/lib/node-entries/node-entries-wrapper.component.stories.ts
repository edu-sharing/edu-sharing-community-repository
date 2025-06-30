import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
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
    Toast,
} from 'ngx-edu-sharing-ui';
import { NodeEntriesWrapperComponent } from './node-entries-wrapper.component';
import {
    AuthenticationServiceMock,
    DefaultColumns,
    NodeEntriesServiceMock,
} from './node-entries-card/node-entries-card.component.stories';
import {
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
        dataSource: dummyDataSource,
        columns: DefaultColumns,
        checkbox: true,
    },
    argTypes: {},
    tags: ['autodocs'],
};

export default entries;
type Story = StoryObj<NodeEntriesWrapperComponent<any>>;
export const EntriesSimpleGrid: Story = {
    args: {},
};
export const EntriesHorizontalGrid: Story = {
    args: {
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
