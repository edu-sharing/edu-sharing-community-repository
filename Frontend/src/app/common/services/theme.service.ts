import { Injectable } from '@angular/core';
import { MaterialCssVariables, MaterialCssVarsService } from 'angular-material-css-vars';
import { ConfigService } from 'ngx-edu-sharing-api';
import { HueValue } from 'angular-material-css-vars/lib/model';

export enum Variable {
    Primary = 'primary',
    Accent = 'accent',
    Warn = 'warn',
}
@Injectable({ providedIn: 'root' })
export class ThemeService {
    constructor(
        private materialCssVarsService: MaterialCssVarsService,
        private configService: ConfigService,
    ) {
        // set defaults
        this.setColor(Variable.Primary, '#48708e');
        this.setColor(Variable.Accent, '#48708e');
        this.setColor(Variable.Warn, '#cd2457');

        this.configService.observeConfig().subscribe(
            (config) => {
                const colors = config.themeColors?.color;
                if (colors) {
                    console.info('apply branding from config', colors);
                    colors.forEach((c) => this.setColor(c.variable, c.value));
                } else {
                    console.info('no branding colors in config, using defaults');
                }
            },
            (error) => {
                console.warn(
                    'Theme service failed to observe config, no branding colors applied',
                    error,
                );
            },
        );
    }
    setColor(variable: Variable | string, color: string) {
        document.documentElement.style.setProperty('--' + variable, color);
        switch (variable) {
            case Variable.Primary:
                this.materialCssVarsService.setPrimaryColor(color);
                this.materialCssVarsService.setVariable(
                    MaterialCssVariables.ForegroundDivider,
                    this.fromPalette(color, '500'),
                );
                break;
            case Variable.Accent:
                this.materialCssVarsService.setAccentColor(color);
                break;
            case Variable.Warn:
                this.materialCssVarsService.setWarnColor(color);
                break;
        }
    }

    private fromPalette(color: string, hueValue: HueValue) {
        const palette = this.materialCssVarsService.getPaletteWithContrastForColor(color);
        const result = palette.filter((p) => p.hue === hueValue)[0].color;
        return `${result.r}, ${result.g}, ${result.b}`;
    }
}
