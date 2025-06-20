import { type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { IconComponent } from './icon.component';
import { ButtonComponent } from '../../../../../src/storybook/button/button.component';
import { SharedModule } from '../../../../../src/app/shared/shared.module';
import { TranslateModule } from '@ngx-translate/core';

const icon: Meta<IconComponent> = {
    title: 'Material/Icons',
    component: IconComponent,
    decorators: [
        moduleMetadata({
            declarations: [], // Prevent duplicate declaration of InfoMessageComponent
            imports: [SharedModule, TranslateModule.forRoot()],
        }),
    ],
    args: {},
};

export default icon;

type Story = StoryObj<ButtonComponent>;
export const DefaultStory: Story = {
    args: {},
};
