import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { EduSharingApiModule, Node, RestConstants } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, Helper, Toast } from 'ngx-edu-sharing-ui';
import { ManageAssignmentNodesComponent } from './manage-assignment-nodes.component';
import {
    DummyNode,
    ToastMock,
    translateProvider,
} from 'src/app/features/mds/mds-editor/storybook-utils';

const dummyNode = DummyNode;
const list: Meta<ManageAssignmentNodesComponent> = {
    title: 'Core UI/Assignment/FileList',
    component: ManageAssignmentNodesComponent,
    decorators: [
        moduleMetadata({
            imports: [EduSharingApiModule, EduSharingUiCommonModule],
            declarations: [],
        }),
        applicationConfig({
            providers: [
                provideAnimations(),
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
    args: {},
    argTypes: {},
    tags: ['autodocs'],
};

export default list;
type Story = StoryObj<ManageAssignmentNodesComponent>;
export const ListEntries: Story = {
    args: {
        nodes: Array(10)
            .fill(DummyNode)
            .map((n: Node, i) => {
                n = Helper.deepCopy(n);
                n.ref.id = 'id_' + i;
                n.title += ' ' + i;
                n.isPublic = Math.random() > 0.5;
                if (Math.random() > 0.5) {
                    n.accessEffective = [RestConstants.ACCESS_CHANGE_PERMISSIONS];
                }
                return n;
            }),
    },
};
export const ListEmpty: Story = {
    args: { nodes: null },
};
