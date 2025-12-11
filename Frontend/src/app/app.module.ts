import { DragDropModule } from '@angular/cdk/drag-drop';
import { LocationStrategy } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { ApplicationRef, DoBootstrap, Injector, NgModule } from '@angular/core';
import { MAT_CHECKBOX_DEFAULT_OPTIONS } from '@angular/material/checkbox';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MAT_RADIO_DEFAULT_OPTIONS } from '@angular/material/radio';
import { MAT_SLIDE_TOGGLE_DEFAULT_OPTIONS } from '@angular/material/slide-toggle';
import { MAT_TOOLTIP_DEFAULT_OPTIONS, MatTooltipDefaultOptions } from '@angular/material/tooltip';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MaterialCssVarsModule } from 'angular-material-css-vars';
import { ResizableModule } from 'angular-resizable-element';
import {
    EDU_SHARING_API_CONFIG,
    EduSharingApiConfigurationParams,
    EduSharingApiModule,
} from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    AppService as AppServiceAbstract,
    EduSharingUiModule,
    KeyboardShortcutsService as KeyboardShortcutsServiceAbstract,
    OptionsHelperService as OptionsHelperServiceAbstract,
    SpinnerComponent,
    Toast as ToastAbstract,
    TranslationsModule,
} from 'ngx-edu-sharing-ui';
import { CustomGlobalExtensionsComponent } from 'src/app/extension/custom-global-component/custom-global-extensions.component';
import { environment } from '../environments/environment';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CoreModule } from './core-module/core.module';
import { extensionDeclarations } from './extension/extension-declarations';
import { extensionImports } from './extension/extension-imports';
import { extensionProviders } from './extension/extension-providers';
import { extensionUiProviders } from './extension/extension-ui-providers';
import { extensionSchemas } from './extension/extension-schemas';
import { DialogsModule } from './features/dialogs/dialogs.module';
import { ManagementDialogsModule } from './features/management-dialogs/management-dialogs.module';
import { MdsModule } from './features/mds/mds.module';
import { ErrorHandlerService } from './main/error-handler.service';
import { AppLocationStrategy } from './main/location-strategy';
import { MainModule } from './main/main.module';
import { CordovaService } from './services/cordova.service';
import { KeyboardShortcutsService } from './services/keyboard-shortcuts.service';
import { OptionsHelperService } from './services/options-helper.service';
import { Toast } from './services/toast';
import { SharedModule } from './shared/shared.module';
import { BApiModule } from 'ngx-edu-sharing-b-api';
import { WrapperComponent } from './web-components/wrapper/app/wrapper.component';
import { MockLocationStrategy } from '@angular/common/testing';
import { WebComponentService } from './main/web-component.service';
import { PreviewSidebarComponent } from './features/preview-sidebar/preview-sidebar.component';
import { WebComponentLocationStrategy } from './main/web-component.utils';
import { RenderingServiceApiModule } from 'ngx-rendering-service-api';

const matTooltipDefaultOptions: MatTooltipDefaultOptions = {
    showDelay: 500,
    hideDelay: 0,
    touchendHideDelay: 0,
};

@NgModule({
    declarations: [
        AppComponent,
        CustomGlobalExtensionsComponent,
        WrapperComponent,
        extensionDeclarations,
    ],
    imports: [
        AppRoutingModule,
        BrowserAnimationsModule,
        BrowserModule,
        CoreModule,
        DialogsModule,
        DragDropModule,
        // forRoot is empty; It is initalized via useFactory!
        RenderingServiceApiModule.forRoot({}),
        EduSharingApiModule.forRoot({}),
        EduSharingUiModule.forRoot({ production: environment.production }, extensionUiProviders),
        BApiModule.forRoot({ rootUrl: environment.bApiUrl || '/edu-sharing/rest/bapi' }),
        extensionImports,
        HttpClientModule,
        MainModule,
        ManagementDialogsModule,
        MaterialCssVarsModule.forRoot({ isAutoContrast: true }),
        MdsModule,
        ResizableModule,
        SharedModule,
        TranslationsModule.forRoot(),
    ],
    providers: [
        { provide: ToastAbstract, useClass: Toast },
        { provide: OptionsHelperServiceAbstract, useClass: OptionsHelperService },
        { provide: KeyboardShortcutsServiceAbstract, useClass: KeyboardShortcutsService },
        { provide: AppServiceAbstract, useClass: CordovaService },
        {
            provide: EDU_SHARING_API_CONFIG,
            deps: [ErrorHandlerService],
            useFactory: (errorHandler: ErrorHandlerService) =>
                ({
                    rootUrl: environment.eduSharingApiUrl,
                    onError: (err, req) => errorHandler.handleError(err, req),
                } as EduSharingApiConfigurationParams),
        },
        { provide: LocationStrategy, useClass: AppLocationStrategy },
        { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
        { provide: MAT_TOOLTIP_DEFAULT_OPTIONS, useValue: matTooltipDefaultOptions },
        { provide: MAT_CHECKBOX_DEFAULT_OPTIONS, useValue: { color: 'primary' } },
        { provide: MAT_RADIO_DEFAULT_OPTIONS, useValue: { color: 'primary' } },
        { provide: MAT_SLIDE_TOGGLE_DEFAULT_OPTIONS, useValue: { color: 'primary' } },
        ...extensionProviders,
        ...extensionUiProviders,

        WebComponentService,
        ErrorHandlerService,
    ].concat(
        environment.webComponentMode
            ? [
                  {
                      provide: LocationStrategy,
                      useClass: WebComponentLocationStrategy,
                  },
              ]
            : [],
    ),
    exports: [AppComponent],
    schemas: [].concat(extensionSchemas),
})
export class AppModule implements DoBootstrap {
    constructor(private injector: Injector) {}

    ngDoBootstrap(appRef: ApplicationRef): void {
        if (environment.webComponentMode) {
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
        } else {
            appRef.bootstrap(AppComponent);
        }
    }
}
