import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import {
    InitialValues,
    MdsValueList,
    MdsViewerWidget,
    MdsWidgetComponent,
} from './mds-widget.component';
import { SharedModule } from '../../../../../../src/app/shared/shared.module';
import { mdsStorybookProviders } from '../../../../../../src/app/features/mds/mds-editor/storybook-utils';
import { BehaviorSubject, Subject } from 'rxjs';
import { DEFAULT, HOME_REPOSITORY, MdsWidget, Node } from 'ngx-edu-sharing-api';
import { provideRouter } from '@angular/router';
import { MdsEditorInstanceServiceAbstract } from '../mds-editor-instance-service.abstract';

export class DefaultWidget implements MdsViewerWidget {
    readonly meetsDynamicCondition = new BehaviorSubject<boolean>(true);
    focusTrigger: Subject<void>;

    constructor(public definition: MdsWidget, private values: string[]) {}

    async getInitalValuesAsync(): Promise<InitialValues> {
        return {
            jointValues: this.values,
        };
    }
    getInitialDisplayValues() {
        return new BehaviorSubject<MdsValueList>(null);
    }
}

const widget: Meta<MdsWidgetComponent> = {
    title: 'Mds/Viewer/Widget',
    component: MdsWidgetComponent,
    decorators: [
        moduleMetadata({
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    args: {},
    argTypes: {},
    tags: ['autodocs'],
};

export default widget;
type Story = StoryObj<MdsWidgetComponent>;
export const Text: Story = {
    args: {
        widget: new DefaultWidget(
            {
                type: 'text',
                caption: 'Text Caption',
            },
            ['Test Value'],
        ),
    },
};
export const Multivalue: Story = {
    args: {
        widget: new DefaultWidget(
            {
                type: 'multivalueFixedBadges',
                caption: 'Text Caption',
            },
            ['Test Value 1', 'Test Value 2'],
        ),
    },
};

export const License: Story = {
    args: {
        widget: new DefaultWidget(
            {
                id: 'license',
                caption: 'License',
            },
            [],
        ),
    },
};

const LRT_WIDGET: MdsWidget = {
    id: 'ccm:educationallearningresourcetype',
    type: 'multivalueFixedBadges',
    caption: 'Inhaltstyp',
    values: [
        { id: 'worksheet', caption: 'Arbeitsblatt' },
        { id: 'video', caption: 'Video' },
    ],
};

const LRT_NODE = {
    ref: { id: 'nodeid', repo: HOME_REPOSITORY, archived: false },
    properties: { [LRT_WIDGET.id]: ['worksheet', 'video'] },
} as unknown as Node;

/**
 * Minimal editor instance. `isSearchable` links are only possible when the raw value ids are
 * reachable, and those come from the node properties via this service.
 */
const mdsEditorInstanceMock: Partial<MdsEditorInstanceServiceAbstract> = {
    mdsId: DEFAULT,
    editorMode: 'viewer',
    values$: new BehaviorSubject(null),
    nodes$: new BehaviorSubject([LRT_NODE]),
    shouldShowExtendedWidgets$: new BehaviorSubject(false),
};

const withNode = applicationConfig({
    providers: [
        provideRouter([]),
        { provide: MdsEditorInstanceServiceAbstract, useValue: mdsEditorInstanceMock },
    ],
});

/** `searchable` in the mds: every value links to a search for that value */
export const MultivalueSearchable: Story = {
    decorators: [withNode],
    args: {
        widget: new DefaultWidget({ ...LRT_WIDGET, isSearchable: true }, ['worksheet', 'video']),
    },
};

/** same widget without `searchable`: plain text, for comparing the rendering */
export const MultivalueNotSearchable: Story = {
    decorators: [withNode],
    args: {
        widget: new DefaultWidget({ ...LRT_WIDGET, isSearchable: false }, ['worksheet', 'video']),
    },
};

/**
 * `searchable` without an editor instance, i.e. no node to read the value ids from. Must fall
 * back to plain text instead of rendering dead links.
 */
export const MultivalueSearchableWithoutNode: Story = {
    decorators: [applicationConfig({ providers: [provideRouter([])] })],
    args: {
        widget: new DefaultWidget({ ...LRT_WIDGET, isSearchable: true }, ['worksheet', 'video']),
    },
};
