import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { EduSharingApiModule, Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, Helper, Toast } from 'ngx-edu-sharing-ui';
import { ManageAssignmentNodesComponent, NodeWithRole } from './manage-assignment-nodes.component';
import {
    DummyNode,
    ToastMock,
    translateProvider,
} from 'src/app/features/mds/mds-editor/storybook-utils';
import { AssignmentBase } from '../manage-assignment/manage-assignment.component';
import { RestConstants } from 'src/app/core-module/core.module';

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
    args: {
        assignment: {
            type: 'SUBMISSION',
        } as AssignmentBase,
    },
    argTypes: {},
    tags: ['autodocs'],
};

export default list;
const nodes = Array(15)
    .fill(DummyNode)
    .map((n: Node, i) => {
        const node = Helper.deepCopy(n) as NodeWithRole;
        node.documentRole = Math.random() > 0.5 ? 'SUPPLEMENTARY' : 'SUBMITTABLE';
        node.isDone = false;
        node.ref.id = 'id_' + i;
        node.title += ' ' + i;
        node.isPublic = Math.random() > 0.5;
        if (Math.random() > 0.5) {
            node.accessEffective = [RestConstants.ACCESS_CHANGE_PERMISSIONS];
        }
        node.properties[RestConstants.CCM_PROP_RESTRICTED_ACCESS] = [(Math.random() > 0.5) + ''];
        return node;
    });
type Story = StoryObj<ManageAssignmentNodesComponent>;
export const ListEntries: Story = {
    args: {
        nodes,
    },
};
export const ListEntriesReadOnly: Story = {
    args: {
        readonly: true,
        nodes,
    },
};
export const ListEmpty: Story = {
    args: { nodes: null },
};
