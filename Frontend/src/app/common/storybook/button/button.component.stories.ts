import { type Meta, type StoryObj } from '@storybook/angular';
import { ButtonComponent, ButtonType } from './button.component';

const button: Meta<ButtonComponent> = {
    title: 'Material/Button',
    component: ButtonComponent,
    args: {
        caption: 'Lorem ipsum',
        color: 'primary',
        type: ButtonType.default,
        disabled: false,
    },
    argTypes: {
        color: {
            control: 'radio',
            options: ['primary', 'accent', 'warn', undefined],
        },
    },
    tags: ['autodocs'],
};

export default button;
type Story = StoryObj<ButtonComponent>;
export const PrimaryButtonDefault: Story = {
    args: {},
};
export const PrimaryButtonDefaultDisabled: Story = {
    args: {
        disabled: true,
    },
};

export const PrimaryButtonFlat: Story = {
    args: {
        type: ButtonType.flat,
    },
};
export const PrimaryButtonFlatDisabled: Story = {
    args: {
        type: ButtonType.flat,
        disabled: true,
    },
};
export const PrimaryButtonRaised: Story = {
    args: {
        type: ButtonType.raised,
    },
};
export const PrimaryButtonRaisedDisabled: Story = {
    args: {
        type: ButtonType.raised,
        disabled: true,
    },
};
export const AccentButtonFlat: Story = {
    args: {
        type: ButtonType.flat,
        color: 'accent',
    },
};
export const AccentButtonFlatDisabled: Story = {
    args: {
        type: ButtonType.flat,
        color: 'accent',
        disabled: true,
    },
};
export const WarnButtonFlat: Story = {
    args: {
        color: 'warn',
        type: ButtonType.flat,
    },
};
export const WarnButtonFlatDisabled: Story = {
    args: {
        color: 'warn',
        type: ButtonType.flat,
        disabled: true,
    },
};

export const NoColorButtonDefault: Story = {
    args: {
        color: undefined,
    },
};

export const NoColorButtonFlat: Story = {
    args: {
        color: undefined,
        type: ButtonType.flat,
    },
};
export const NoColorButtonFlatDisabled: Story = {
    args: {
        color: undefined,
        type: ButtonType.flat,
        disabled: true,
    },
};
export const NoColorButtonRaised: Story = {
    args: {
        color: undefined,
        type: ButtonType.raised,
    },
};
