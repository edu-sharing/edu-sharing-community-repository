import { Injectable, NgZone } from '@angular/core';
import { MaterialCssVarsService } from 'angular-material-css-vars';
import { ColorHelper } from '../../core-module/ui/color-helper';
import { ConfigService } from 'ngx-edu-sharing-api';

export enum Variable {
    Primary = 'primary',
    Accent = 'accent',
}
@Injectable({ providedIn: 'root' })
export class ThemeService {
    constructor(
        private materialCssVarsService: MaterialCssVarsService,
        private configService: ConfigService,
    ) {
        this.configService.observeConfig().subscribe(
            (config) => {
                const colors = config.themeColors?.color;
                if (colors) {
                    console.info('apply branding from config', colors);
                    colors.forEach((c) => this.setColor(c.variable, c.value));
                } else {
                    console.info('no branding colors in config, using defaults');
                    this.setColor(Variable.Primary, '#48708e');
                    this.setColor(Variable.Accent, '#48708e');
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
                break;
            case Variable.Accent:
                this.materialCssVarsService.setAccentColor(color);
                break;
        }
    }
}
