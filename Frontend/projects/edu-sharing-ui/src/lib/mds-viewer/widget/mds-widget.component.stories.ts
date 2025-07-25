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
import { MdsWidget } from 'ngx-edu-sharing-api';
import { TranslateService } from '@ngx-translate/core';

export class DefaultWidget implements MdsViewerWidget {
    focusTrigger: Subject<void>;

    constructor(public definition: MdsWidget, private values: string[]) {}

    getFormattedValue(value: string[], basicType: string, translation: TranslateService): string[] {
        return [''];
    }
    async getInitalValuesAsync(): Promise<InitialValues> {
        return {
            jointValues: this.values,
        };
    }
    getInitialDisplayValues() {
        return new BehaviorSubject<MdsValueList>(null);
    }

    getBasicType(flat?: boolean): string {
        return '';
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
