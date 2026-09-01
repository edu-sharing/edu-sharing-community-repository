import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { argbFromHex, Hct, hexFromArgb, TonalPalette } from '@material/material-color-utilities';
import { HueValue, MaterialCssVarsService } from 'angular-material-css-vars';
import { ConfigService, ConfigThemeColor, ConfigThemeColors } from 'ngx-edu-sharing-api';
import {
    AccessibilityService,
    ColorHelper,
    DarkModeSetting,
    EDU_SHARING_UI_CONFIG,
    EduSharingUiConfiguration,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { combineLatest, fromEvent } from 'rxjs';
import { distinctUntilChanged, first, map, startWith } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export enum Variable {
    Primary = 'primary',
    Accent = 'accent',
    Warn = 'warn',
}

/** HCT tone used to lift light brand colors onto dark surfaces (matches the default dark primary). */
const DARK_TONE = 80;
/** HCT tone for adapting a custom (bright) surface color to dark mode: a dark tint, hue kept. */
const DARK_SURFACE_TONE = 30;
@Injectable({ providedIn: 'root' })
export class ThemeService {
    private materialCssVarsService = inject(MaterialCssVarsService);
    private configService = inject(ConfigService);
    private uiConfig = inject<EduSharingUiConfiguration>(EDU_SHARING_UI_CONFIG);
    private accessibility = inject(AccessibilityService);
    private router = inject(Router);

    /** Whether dark mode is currently active. Readable from anywhere via the injected service. */
    readonly isDarkMode = signal(false);

    /** Latest light-mode branding colors from the backend config, re-applied on every dark-mode toggle. */
    private configColors: Array<ConfigThemeColor> | null = null;

    /**
     * Latest dark-mode branding colors from the backend config (the `themeColors` entry with
     * theme="dark"). When present, they are applied as-is in dark mode instead of deriving lightened
     * variants from {@link configColors}.
     */
    private configDarkColors: Array<ConfigThemeColor> | null = null;

    /**
     * Whether the app renders into a document it does not own: the web-component build, or an
     * `/components/embed/*` route meant for an iframe. Nothing is scoped by a shadow root, so the
     * theme would repaint the host page. These contexts stay light unless `?theme=` says otherwise.
     */
    private readonly isExternalContext =
        environment.webComponentMode ||
        window.location.pathname.includes('/' + UIConstants.ROUTER_PREFIX + 'embed');

    constructor() {
        // Paint the synchronous default first (avoids a flash before the observers resolve),
        // then start the reactive resolution. registerDarkMode must run last so its config-aware
        this.initWithDefaults();
        this.registerDarkMode();
    }

    /**
     * Applies the dark/light theme by combining the user's accessibility setting with the
     * browser's `prefers-color-scheme`. When the setting is `auto`, live browser changes are
     * followed; `light`/`dark` force a fixed value, overriding the browser default.
     *
     * A `?theme=auto|dark|light` query param always takes priority over the stored preference
     * it is presentation-only and not persisted, so removing
     * the param reverts to the user's saved preference. In an {@link isExternalContext} the stored
     * preference is ignored in favor of light.
     */
    private registerDarkMode() {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        const browserDark$ = fromEvent<MediaQueryListEvent>(mediaQuery, 'change').pipe(
            map((event) => event.matches),
            startWith(mediaQuery.matches),
        );

        // The router serves only as a change trigger; the value itself is read from the browser
        // location, which is the embedding page's URL in the web-component build.
        const urlTheme$ = this.router.routerState.root.queryParamMap.pipe(
            map(() => this.getThemeFromUrl()),
            distinctUntilChanged(),
        );

        combineLatest([this.accessibility.observe('darkMode'), browserDark$, urlTheme$])
            .pipe(
                map(([storedMode, browserDark, urlTheme]) =>
                    this.resolveIsDark(
                        urlTheme ?? (this.isExternalContext ? 'light' : storedMode),
                        browserDark,
                    ),
                ),
            )
            .subscribe((isDark) => {
                this.applyTheme(isDark);
            });
    }

    /**
     * Validates a raw `theme` query-param value against the {@link DarkModeSetting} values.
     * Returns `null` when absent or invalid, so callers can fall back to the stored preference.
     */
    private toThemeSetting(value: string | null): DarkModeSetting | null {
        return value === 'auto' || value === 'light' || value === 'dark' ? value : null;
    }

    private getThemeFromUrl(): DarkModeSetting | null {
        return this.toThemeSetting(new URLSearchParams(window.location.search).get('theme'));
    }

    /** Resolves an effective dark-mode setting into a boolean, following the browser when `auto`. */
    private resolveIsDark(mode: DarkModeSetting, browserDark: boolean): boolean {
        return mode === 'dark' ? true : mode === 'light' ? false : browserDark;
    }

    initWithDefaults() {
        const browserDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        // honor a ?theme= override on the first synchronous paint to avoid flashing the browser
        // default before the observers in registerDarkMode resolve
        const urlTheme = this.getThemeFromUrl();
        this.accessibility
            .observe('darkMode')
            .pipe(first())
            .subscribe((storedMode) => {
                this.applyTheme(
                    this.resolveIsDark(
                        urlTheme ?? (this.isExternalContext ? 'light' : storedMode),
                        browserDark,
                    ),
                );
            });
        this.fetchConfig();
    }

    /**
     * Applies the hardcoded default colors for the given theme, then overrides them with the
     * latest backend config colors (if any). Called on initial load and on every dark-mode toggle
     * so the backend branding survives toggles instead of reverting to the defaults.
     */
    private applyTheme(isDark: boolean) {
        this.isDarkMode.set(isDark);
        this.materialCssVarsService.setDarkTheme(isDark);
        this.applyDefaultColors(isDark);
        const colors = this.resolveConfigColors(isDark);
        if (colors) {
            this.applyFromConfigColors(colors);
        }
    }

    /**
     * Picks the branding colors to apply for the given mode. In dark mode, an explicit dark color
     * set (the `themeColors` entry with theme="dark") wins and is used as-is; otherwise the dark
     * variants are derived from the light colors client-side (see {@link toDarkColors}).
     */
    private resolveConfigColors(isDark: boolean): Array<ConfigThemeColor> | null {
        if (isDark) {
            if (this.configDarkColors) {
                return this.configDarkColors;
            }
            return this.configColors ? this.toDarkColors(this.configColors) : null;
        }
        return this.configColors;
    }

    /**
     * Finds the configured color set for the given mode. The light set is the entry whose `theme`
     * attribute is absent or `"light"`; the dark set is the entry with `theme="dark"`.
     */
    private findThemeColors(
        themeColors: Array<ConfigThemeColors> | undefined,
        theme: 'light' | 'dark',
    ): ConfigThemeColors | undefined {
        return themeColors?.find((c) =>
            theme === 'dark' ? c.theme === 'dark' : !c.theme || c.theme === 'light',
        );
    }

    /**
     * Transforms light brand colors into dark-surface-appropriate variants (same hue, lighter tone).
     * Only the theme palette colors (Primary, Accent, Warn) are converted; any other config color is
     * left untouched.
     */
    private toDarkColors(colors: Array<ConfigThemeColor>): Array<ConfigThemeColor> {
        const convertible: Array<string> = [Variable.Primary, Variable.Accent, Variable.Warn];
        return colors.map((c) => {
            if (!c.value || !c.variable || !convertible.includes(c.variable)) {
                return c;
            }
            return { ...c, value: this.toDarkColor(c.value) };
        });
    }

    private toDarkColor(hex: string): string {
        try {
            const palette = TonalPalette.fromInt(argbFromHex(hex));
            return hexFromArgb(palette.tone(DARK_TONE));
        } catch {
            // non-hex / unparseable config value: leave untouched
            return hex;
        }
    }

    /**
     * Adapts an arbitrary custom background color for use as a surface in dark mode. Colors that
     * are brighter than {@link DARK_SURFACE_TONE} are pulled down to that tone (keeping hue and
     * chroma) so they read as a dark surface with the theme's (light) text; colors that are
     * already dark are returned unchanged, since they already work on the dark theme (with the
     * bright text applied by `.bright-color`). Non-hex inputs (e.g. `rgb(...)`) are normalized to
     * hex first; values that cannot be parsed (e.g. named colors) are returned untouched.
     *
     * @param color the configured background color (hex, rgb(), …)
     */
    toDarkSurfaceColor(color: string): string {
        const hex = ThemeService.toHex(color);
        if (!hex) {
            return color;
        }
        try {
            const hct = Hct.fromInt(argbFromHex(hex));
            if (hct.tone <= DARK_SURFACE_TONE) {
                // already dark enough to serve as a dark-mode surface
                return color;
            }
            hct.tone = DARK_SURFACE_TONE;
            return hexFromArgb(hct.toInt());
        } catch {
            return color;
        }
    }

    /**
     * Normalizes a CSS color to a `#rrggbb` hex string, or returns null when it cannot be parsed
     * (handles `#rrggbb` and `rgb(...)`; named colors and other formats yield null).
     */
    private static toHex(color: string): string | null {
        if (!color) {
            return null;
        }
        const rgb = ColorHelper.cssColorToRgb(color.trim());
        return rgb ? ColorHelper.rgbToHex(rgb) : null;
    }

    private applyDefaultColors(isDark: boolean) {
        if (isDark) {
            this.setColor(Variable.Primary, '#96cdf8');
            this.setColor(Variable.Accent, '#96cdf8');
            this.setColor(Variable.Warn, '#ff6b9d');
        } else {
            this.setColor(Variable.Primary, '#48708e');
            this.setColor(Variable.Accent, '#48708e');
            this.setColor(Variable.Warn, '#cd2457');
        }
    }

    private fetchConfig() {
        this.configService.observeConfig().subscribe({
            next: (config) => {
                // themeColors is a list of color sets discriminated by their `theme` attribute:
                // an entry with no theme (or theme="light") is the light set, theme="dark" the dark set.
                const lightColors = this.findThemeColors(config.themeColors, 'light')?.color;
                const darkColors = this.findThemeColors(config.themeColors, 'dark')?.color;
                this.setFavicon(config.favicon, config.appleTouchIcon);
                if (lightColors || darkColors) {
                    // When only the light set is configured, we derive the dark variants from it
                    // client-side as a fallback (see toDarkColors).
                    this.configColors = lightColors ?? null;
                    this.configDarkColors = darkColors ?? null;
                    this.applyTheme(this.isDarkMode());
                }
            },
            error: (error) => {
                console.warn(
                    'Theme service failed to observe config, no branding colors applied',
                    error,
                );
            },
        });
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
