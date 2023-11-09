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
        <button
            *ngIf="type === ButtonType.default"
            mat-button
            [color]="color"
            [disabled]="disabled"
        >
            {{ caption }}
        </button>
        <button
            *ngIf="type === ButtonType.flat"
            mat-flat-button
            [color]="color"
            [disabled]="disabled"
        >
            {{ caption }}
        </button>
        <button
            *ngIf="type === ButtonType.raised"
            mat-raised-button
            [color]="color"
            [disabled]="disabled"
        >
            {{ caption }}
        </button>
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
