import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { NgxColorsColor, NgxColorsModule } from 'ngx-colors';
import { ConfigService } from 'ngx-edu-sharing-api';
import { ColorHelper } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { TopicPageGlobalService } from '../../../shared/services/topic-page-global.service';

@Component({
    selector: 'es-color-picker',
    imports: [CommonModule, FormsModule, MatButton, NgxColorsModule, TranslateModule],
    templateUrl: './color-picker.component.html',
    styleUrls: ['./color-picker.component.scss'],
})
export class ColorPickerComponent implements OnInit {
    private _selectedColor: string = '#ffffff';
    private _initialColor: string | null = null;

    @Input() addTransparency: boolean = false;
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
    @Input() customColor: string = '';
    @Input() customColorPosition: 'start' | 'end' = 'end';
    @Output() colorChange: EventEmitter<string> = new EventEmitter<string>();

    get internalColor(): string {
        return this._selectedColor;
    }
    set internalColor(value: string) {
        this._selectedColor = value;
    }
    protected palette: any[] = [];

    constructor(
        private configService: ConfigService,
        private topicPageGlobalService: TopicPageGlobalService,
    ) {}

    /**
     * Initializes the component by retrieving a defined color palette or the default colors and generating the palette.
     */
    async ngOnInit() {
        // check whether a custom color palette is defined, otherwise use the default colors
        if (this.topicPageGlobalService.getCustomColorPalette()?.length) {
            // pushing is necessary to avoid conflicts among different instances of the color picker
            this.topicPageGlobalService
                .getCustomColorPalette()
                .forEach((color: string | NgxColorsColor) => {
                    this.palette.push(color);
                });
        } else {
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
        // add a custom color to the palette (if not already included)
        if (this.customColor) {
            const customColorIncluded = this.palette.find((c) => {
                return (
                    c === this.customColor ||
                    c?.preview === this.customColor ||
                    c?.variants?.includes(this.customColor)
                );
            });
            if (!customColorIncluded) {
                if (this.customColorPosition === 'start') {
                    this.palette.unshift(this.customColor);
                } else {
                    this.palette.push(this.customColor);
                }
            }
        }
        // workaround to reset the color to the default undefined color
        if (this.addTransparency && !this.palette.includes(undefined)) {
            this.palette.push(undefined);
        }
    }

    /**
     * Emits the color change event when the color is changed.
     * Only emits if the color actually changed from the initial value.
     */
    onColorChange(): void {
        if (
            this._initialColor &&
            this._selectedColor?.toLowerCase() !== this._initialColor?.toLowerCase()
        ) {
            this.colorChange.emit(this.selectedColor);
            // update the initial value for further changes
            this._initialColor = this._selectedColor;
        }
    }
}
