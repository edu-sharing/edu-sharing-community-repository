import { Component, Input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { ThemePalette } from '@angular/material/core';
import { CommonModule } from '@angular/common';

export enum ButtonType {
    default,
    flat,
    raised,
}

@Component({
    selector: 'es-storybook-button',
    standalone: true,
    template: `
        @if (type === ButtonType.default) {
        <button mat-button [color]="color" [disabled]="disabled">
            {{ caption }}
        </button>
        } @if (type === ButtonType.flat) {
        <button mat-flat-button [color]="color" [disabled]="disabled">
            {{ caption }}
        </button>
        } @if (type === ButtonType.raised) {
        <button mat-raised-button [color]="color" [disabled]="disabled">
            {{ caption }}
        </button>
        }
    `,
    imports: [MatButtonModule, CommonModule],
})
export class ButtonComponent {
    @Input() type: ButtonType;
    @Input() caption: string;
    @Input() color: ThemePalette;
    @Input() disabled: boolean;
    readonly ButtonType = ButtonType;
}
