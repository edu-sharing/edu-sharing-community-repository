import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThemeService, Variable } from '../app/services/theme.service';

/***
 @TODO: This component was intented to be injected in each storybook story to handle global styles
 However, it currently can not access the material css provider
 Unhandled Promise rejection: NullInjectorError: No provider for InjectionToken Mat Css Config! ; Zone: <root> ; Task: Promise.then ; Value: NullInjectorError: NullInjectorError: No provider for InjectionToken Mat Css Config!
 **/
@Component({
    selector: 'es-storybook-material-wrapper',
    standalone: true,
    template: ``,
    imports: [CommonModule],
})
export class StorybookMaterialWrapperComponent {
    @Input() set theme(theme: string) {
        console.log(theme);
        this.themeService.setColor(Variable.Primary, '#ff00000');
    }
    constructor(private themeService: ThemeService) {}
}
