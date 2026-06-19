import { Injectable, inject } from '@angular/core';
import { MaterialCssVarsService } from 'angular-material-css-vars';
import { HueValue } from 'angular-material-css-vars';
import { ConfigService, ConfigThemeColor } from 'ngx-edu-sharing-api';
import { EDU_SHARING_UI_CONFIG, EduSharingUiConfiguration } from 'ngx-edu-sharing-ui';

export enum Variable {
    Primary = 'primary',
    Accent = 'accent',
    Warn = 'warn',
}
@Injectable({ providedIn: 'root' })
export class ThemeService {
    private materialCssVarsService = inject(MaterialCssVarsService);
    private configService = inject(ConfigService);
    private uiConfig = inject<EduSharingUiConfiguration>(EDU_SHARING_UI_CONFIG);

    constructor() {
        // set defaults
        this.initWithDefaults();
    }

    initWithDefaults() {
        this.setColor(Variable.Primary, '#48708e');
        this.setColor(Variable.Accent, '#48708e');
        this.setColor(Variable.Warn, '#cd2457');
        this.setViaConfig();
    }

    private setViaConfig() {
        this.configService.observeConfig().subscribe(
            (config) => {
                const colors = config.themeColors?.color;
                this.setFavicon(config.favicon, config.appleTouchIcon);
                if (colors) {
                    this.applyFromConfigColors(colors);
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

    applyFromConfigColors(colors: Array<ConfigThemeColor>) {
        colors.forEach((c) => this.setColor(c.variable, c.value));
    }

    private setFavicon(favicon?: string, appleTouchIcon?: string) {
        if (favicon?.trim()) {
            const iconRef = document.querySelector('html head link[rel=icon]');
            (iconRef as HTMLLinkElement).href = favicon;
        }
        if (appleTouchIcon?.trim()) {
            const appleRef = document.querySelector('html head link[rel=apple-touch-icon]');
            (appleRef as HTMLLinkElement).href = appleTouchIcon;
        }
    }

    setColor(variable: Variable | string, color: string) {
        document.documentElement.style.setProperty('--' + variable, color);
        switch (variable) {
            case Variable.Primary:
                this.materialCssVarsService.setPrimaryColor(color);
                /*this.materialCssVarsService.setVariable(
                    MaterialCssVariables.ForegroundDivider,
                    this.fromPalette(color, '500'),
                );*/
                if (!this.uiConfig.isEmbedded) {
                    document
                        // @TODO: This fails in web component context: TypeError: Cannot read properties of null (reading 'setAttribute')
                        .querySelector('meta[name="theme-color"]')
                        ?.setAttribute('content', color);
                }
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
