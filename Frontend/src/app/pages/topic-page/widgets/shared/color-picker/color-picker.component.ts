import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { NgxColorsColor, NgxColorsModule } from 'ngx-colors';
import { ConfigService } from 'ngx-edu-sharing-api';
import { ColorHelper } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';

@Component({
    selector: 'es-color-picker',
    imports: [CommonModule, FormsModule, MatButton, NgxColorsModule, TranslateModule],
    templateUrl: './color-picker.component.html',
    styleUrls: ['./color-picker.component.scss'],
})
export class ColorPickerComponent implements OnInit {
    private _selectedColor: string = '#ffffff';
    private _initialColor: string | null = null;

    @Input()
    get selectedColor(): string {
        return this._selectedColor;
    }
    set selectedColor(value: string) {
        this._selectedColor = value;
        // save the initial value for further changes
        if (this._initialColor === null) {
            this._initialColor = value;
        }
    }
    @Input() disabled: boolean = false;
    @Input() acceptLabel: string = 'APPLY';
    @Input() cancelLabel: string = 'CANCEL';
    @Input() colorLabel: string;
    @Input() customClass: string = '';
    @Output() colorChange: EventEmitter<string> = new EventEmitter<string>();

    get internalColor(): string {
        return this._selectedColor;
    }
    set internalColor(value: string) {
        this._selectedColor = value;
    }
    protected palette: NgxColorsColor[] = [];

    constructor(private configService: ConfigService) {}

    /**
     * Initializes the component by retrieving the default colors and generating the color palette.
     */
    async ngOnInit() {
        const colors = await this.configService.get<string[]>(
            'collections.colors',
            RestConstants.DEFAULT_COLLECTION_COLORS,
        );
        colors.forEach((c) => {
            this.palette.push({
                preview: c,
                variants: ColorHelper.generateHslVariants(c, 7).reverse(),
            });
        });
    }

    /**
     * Emits the color change event when the color is changed.
     * Only emits if the color actually changed from the initial value.
     */
    onColorChange(): void {
        if (this._initialColor !== null && this._selectedColor !== this._initialColor) {
            this.colorChange.emit(this.selectedColor);
            // update the initial value for further changes
            this._initialColor = this._selectedColor;
        }
    }
}
