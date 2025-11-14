import {
    applicationConfig,
    argsToTemplate,
    type Meta,
    moduleMetadata,
    type StoryObj,
} from '@storybook/angular';
import { ShareDialogChooseDateComponent } from './choose-date.component';
import { SharedModule } from '../../../../../../shared/shared.module';
import { mdsStorybookProviders, ToastMock } from '../../../../../mds/mds-editor/storybook-utils';
import { Toast } from 'ngx-edu-sharing-ui';

const date: Meta<ShareDialogChooseDateComponent> = {
    title: 'Material/DateTime',
    component: ShareDialogChooseDateComponent,
    decorators: [
        moduleMetadata({
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders.concat([{ provide: Toast, useClass: ToastMock }]),
        }),
    ],
    render: ({ ...args }) => ({
        props: { ...args },
        template: `<es-share-dialog-choose-date ${argsToTemplate(
            args,
        )} (dateTimeChange)="date = $event"></es-share-dialog-choose-date>Choosen Date: {{ date | formatDate:{relative: false, time: true} }}`,
    }),
    args: {
        dateTime: new Date().getTime(),
    },
};

export default date;

type Story = StoryObj<ShareDialogChooseDateComponent>;
export const DateFromLimit: Story = {
    args: {
        from: new Date().getTime() - 86400000,
    },
};
export const DateToLimit: Story = {
    args: {
        to: new Date().getTime() + 86400000,
    },
};
export const DateFromToLimit: Story = {
    args: {
        from: new Date().getTime() - 86400000,
        to: new Date().getTime() + 86400000,
    },
};
export const DateNoLimit: Story = {
    args: {},
};
