import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import {
    applicationConfig,
    componentWrapperDecorator,
    type Meta,
    moduleMetadata,
    type StoryObj,
} from '@storybook/angular';
import { ApiRequestConfiguration, UserEvent } from 'ngx-edu-sharing-api';
import { Toast } from 'ngx-edu-sharing-ui';
import { DashboardInteractivityStreamComponent } from './dashboard-interactivity-stream.component';
import {
    DummyNode,
    DummyUser,
    ToastMock,
    translateProvider,
} from '../../mds/mds-editor/storybook-utils';

const stream: Meta<DashboardInteractivityStreamComponent> = {
    title: 'Dashboard/Interactivity Stream',
    component: DashboardInteractivityStreamComponent,
    decorators: [
        componentWrapperDecorator((story) => `<div style="max-width: 300px;">${story}</div>`),
        moduleMetadata({
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
        events: Array.from({ length: 6 }, () => {
            return {
                eventType: 'EDIT_MATERIAL',
                initiator: new DummyUser(),
                node: DummyNode,
                timestamp: (new Date().getTime() -
                    Math.floor(Math.pow(Math.random(), 3) * 1000 * 3600 * 24 * 40)) as unknown,
            } as UserEvent;
        }),
    },
    argTypes: {},
    tags: ['autodocs'],
};

export default stream;
type Story = StoryObj<DashboardInteractivityStreamComponent>;
export const StreamSharing: Story = {
    args: {},
};
