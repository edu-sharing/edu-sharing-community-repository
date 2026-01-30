import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { AuthenticationService, ConfigService, IamV1Service, Node } from 'ngx-edu-sharing-api';
import { I18N_CONFIG, I18nConfig, Toast } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../shared/shared.module';
import {
    AuthenticationServiceMock,
    ConfigServiceMock,
    DummyNode,
    IamServiceMock,
    mdsStorybookProviders,
    ToastMock,
} from '../mds/mds-editor/storybook-utils';
import { ShortcutEntriesComponent } from './shortcut-entries.component';

// Additional providers necessary for selected components
const additionalProviders = [{ provide: IamV1Service, useClass: IamServiceMock }];

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const shortcutEntries: Meta<ShortcutEntriesComponent> = {
    title: 'Dashboard/Shortcut entries',
    component: ShortcutEntriesComponent,
    decorators: [
        moduleMetadata({
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders.concat(additionalProviders),
        }),
    ],
    args: {},
    argTypes: {},
    tags: ['autodocs'],
};

export default shortcutEntries;

// simulate a touch device: chrome -> three points -> add device type -> desktop (touch)
type Story = StoryObj<ShortcutEntriesComponent>;
export const Landing_page_view: Story = {
    args: {},
};
const dummyNode = {
    ...DummyNode,
    ...{
        icon: {
            url: 'data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPCEtLSBHZW5lcmF0b3I6IEFkb2JlIElsbHVzdHJhdG9yIDIwLjEuMCwgU1ZHIEV4cG9ydCBQbHVnLUluIC4gU1ZHIFZlcnNpb246IDYuMDAgQnVpbGQgMCkgIC0tPgo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9IkViZW5lXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4IgoJIHZpZXdCb3g9IjAgMCAxNiAxNiIgc3R5bGU9ImVuYWJsZS1iYWNrZ3JvdW5kOm5ldyAwIDAgMTYgMTY7IiB4bWw6c3BhY2U9InByZXNlcnZlIj4KPHN0eWxlIHR5cGU9InRleHQvY3NzIj4KCS5zdDB7ZmlsbDojNjVCNDg0O30KPC9zdHlsZT4KPHBhdGggY2xhc3M9InN0MCIgZD0iTTAsMnYxMmgxNlYySDB6IE0zLDEzSDF2LTJoMlYxM3ogTTMsOUgxVjdoMlY5eiBNMyw1SDFWM2gyVjV6IE0xMiwxM0g0VjNoOFYxM3ogTTE1LDEzaC0ydi0yaDJWMTN6IE0xNSw5aC0yVjcKCWgyVjl6IE0xNSw1aC0yVjNoMlY1eiBNNiw1djZsNC0zTDYsNXoiLz4KPC9zdmc+Cg==',
        },
    },
};
export const Modal_view: Story = {
    args: {
        node: dummyNode as Node,
    },
};
