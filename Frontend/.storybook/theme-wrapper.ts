import { Component, importProvidersFrom, Input } from '@angular/core';
import { Decorator } from '@storybook/angular';
import { useGlobals } from 'storybook/preview-api';
import { MaterialCssVarsModule, MaterialCssVarsService } from 'angular-material-css-vars';
import { ThemeService, Variable } from '../src/app/services/theme.service';

/** Tracks the last seen dark-mode value so we only re-select the Theme picker on a real toggle. */
let lastDarkMode: string | undefined;

export interface Themes {
    [name: string]: Theme;
}

type Theme = {
    [color in Variable]: string;
};

@Component({
    selector: 'es-storybook-theme-wrapper',
    standalone: true,
    template: '',
})
export class StorybookThemeWrapperComponent {
    @Input() set theme(theme: Theme) {
        for (const [color, value] of Object.entries(theme ?? {})) {
            this.themeService.setColor(color, value);
        }
    }

    @Input() set darkMode(isDark: boolean) {
        this.materialCssVarsService.setDarkTheme(!!isDark);
    }

    constructor(
        private themeService: ThemeService,
        private materialCssVarsService: MaterialCssVarsService,
    ) {}
}

/**
 * A decorator function that applies the material theme set in `globalTypes` to to the story.
 */
export function withTheme<TArgs = unknown>(themes: Themes): Decorator<TArgs> {
    return (storyFn, storyContext) => {
        const [globals, updateGlobals] = useGlobals();
        const darkMode = globals['darkMode'];
        let themeKey = globals['theme'];
        // On a dark-mode toggle, move the Theme picker to the matching palette. Only on an actual
        // change, so a manual Theme selection isn't overridden between toggles.
        if (darkMode !== lastDarkMode) {
            lastDarkMode = darkMode;
            themeKey = darkMode === 'dark' ? 'darkModeColors' : 'default';
            updateGlobals({ theme: themeKey });
        }

        const story = storyFn();

        story.moduleMetadata ??= {};
        story.moduleMetadata.imports ??= [];
        story.moduleMetadata.imports.push(StorybookThemeWrapperComponent);

        story.applicationConfig ??= { providers: [] };
        story.applicationConfig.providers.push(
            importProvidersFrom(MaterialCssVarsModule.forRoot({ isAutoContrast: true })),
        );

        // Storybook also assigns story props onto the instance of the story's `component`, so the
        // prop names must not collide with component members (e.g. an injected `theme` service).
        story.props ??= {};
        story.props.storybookTheme = themes[themeKey];
        story.props.storybookDarkMode = darkMode === 'dark';

        story.template =
            `<es-storybook-theme-wrapper [theme]="storybookTheme" [darkMode]="storybookDarkMode">` +
            `</es-storybook-theme-wrapper>` +
            story.template;

        return story;
    };
}
