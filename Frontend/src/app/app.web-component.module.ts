import { ApplicationRef, DoBootstrap, Injector, NgModule } from '@angular/core';
import { ActionbarComponent, SpinnerComponent } from 'ngx-edu-sharing-ui';
import { extensionSchemas } from './extension/extension-schemas';
import { WrapperComponent } from './web-components/wrapper/app/wrapper.component';
import { WebComponentService } from './main/web-component.service';
import { PreviewSidebarComponent } from './features/preview-sidebar/preview-sidebar.component';
import { AppModule, Providers } from './app.module';
import { GenericWidgetComponent } from './pages/topic-page/widgets/generic-widget/generic-widget.component';
import { AppComponent } from './app.component';
import { WebComponentOnlyService } from './main/web-component-only.service';

@NgModule({
    imports: [AppModule],
    providers: Providers.concat(WebComponentOnlyService),
    exports: [AppComponent],
    schemas: [].concat(extensionSchemas),
})
export class WebComponentModule implements DoBootstrap {
    constructor(private injector: Injector) {}

    ngDoBootstrap(_: ApplicationRef): void {
        console.info('web component __env', (window as any).__env);
        this.injector
            .get(WebComponentService)
            .registerWebComponent('edu-sharing-app', WrapperComponent);
        this.injector
            .get(WebComponentService)
            .registerWebComponent('edu-sharing-spinner', SpinnerComponent);
        this.injector
            .get(WebComponentService)
            .registerWebComponent('edu-sharing-actionbar', ActionbarComponent);
        this.injector
            .get(WebComponentService)
            .registerWebComponent('edu-sharing-preview-sidebar', PreviewSidebarComponent);
        this.injector
            .get(WebComponentService)
            .registerWebComponent('edu-sharing-generic-widget', GenericWidgetComponent);
    }
}
