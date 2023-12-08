import { Component, Input, importProvidersFrom } from '@angular/core';
import { Decorator } from '@storybook/angular';
import { MaterialCssVarsModule } from 'angular-material-css-vars';
import { ThemeService, Variable } from '../src/app/services/theme.service';

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

    constructor(private themeService: ThemeService) {}
}

/**
 * A decorator function that applies the material theme set in `globalTypes` to to the story.
 */
export function withTheme<TArgs = unknown>(themes: Themes): Decorator<TArgs> {
    return (storyFn, storyContext) => {
        const story = storyFn();

        story.moduleMetadata ??= {};
        story.moduleMetadata.imports ??= [];
        story.moduleMetadata.imports.push(StorybookThemeWrapperComponent);

        story.applicationConfig ??= { providers: [] };
        story.applicationConfig.providers.push(
            importProvidersFrom(MaterialCssVarsModule.forRoot({ isAutoContrast: true })),
        );

        story.props ??= {};
        story.props.theme = themes[storyContext.globals['theme']];

        story.template =
            `<es-storybook-theme-wrapper [theme]="theme">` +
            `</es-storybook-theme-wrapper>` +
            story.template;

        return story;
    };
}
