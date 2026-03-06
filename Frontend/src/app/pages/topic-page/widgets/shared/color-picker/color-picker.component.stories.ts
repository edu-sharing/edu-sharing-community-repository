import { applicationConfig, Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { provideAnimations } from '@angular/platform-browser/animations';
import { mdsStorybookProviders } from '../../../../../features/mds/mds-editor/storybook-utils';
import { ColorPickerComponent } from './color-picker.component';

const colorpicker: Meta<ColorPickerComponent> = {
    title: 'Core UI/ColorPicker',
    component: ColorPickerComponent,
    decorators: [
        moduleMetadata({
            imports: [],
            declarations: [],
        }),
        applicationConfig({
            providers: [provideAnimations(), mdsStorybookProviders],
        }),
    ],
    args: {},
    argTypes: {},
    tags: ['autodocs'],
};

export default colorpicker;
type Story = StoryObj<ColorPickerComponent>;
export const Colorpicker: Story = {
    args: {},
};
