import { MatSlideToggle, MatSlideToggleModule } from '@angular/material/slide-toggle';
import { argsToTemplate, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';

interface MatSlideToggleExtended extends MatSlideToggle {
    label: string;
    class: string;
}

const slideToggle: Meta<MatSlideToggleExtended> = {
    title: 'Material/Slide toggle',
    component: MatSlideToggle,
    decorators: [moduleMetadata({ imports: [MatSlideToggleModule] })],
    args: {
        label: 'Slide me!',
        checked: true,
        disabled: false,
    },
    argTypes: {
        color: {
            control: 'radio',
            options: ['primary', 'accent', 'warn'],
        },
    },
    render: ({ label, ...args }: MatSlideToggleExtended) => ({
        props: { label, ...args },
        template: `<mat-slide-toggle ${argsToTemplate(args)}>{{ label }}</mat-slide-toggle>`,
    }),
    tags: ['autodocs'],
};

export default slideToggle;
type Story = StoryObj<MatSlideToggleExtended>;

export const Default: Story = {
    args: {},
};

export const Disabled: Story = {
    args: {
        disabled: true,
    },
};

export const PrimaryBright: Story = {
    args: {
        class: 'mat-primary-bright',
    },
    parameters: {
        backgrounds: { default: 'dark' },
    },
};
