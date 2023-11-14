import { DragDropModule } from '@angular/cdk/drag-drop';
import { LocationStrategy } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MAT_TOOLTIP_DEFAULT_OPTIONS, MatTooltipDefaultOptions } from '@angular/material/tooltip';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { InMemoryCache } from '@apollo/client/core';
import { MaterialCssVarsModule } from 'angular-material-css-vars';
import { ResizableModule } from 'angular-resizable-element';
import { APOLLO_OPTIONS, ApolloModule } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import {
    EDU_SHARING_API_CONFIG,
    EduSharingApiConfigurationParams,
    EduSharingApiModule,
} from 'ngx-edu-sharing-api';
import { EduSharingGraphqlModule } from 'ngx-edu-sharing-graphql';
import {
    AppService as AppServiceAbstract,
    EduSharingUiModule,
    KeyboardShortcutsService as KeyboardShortcutsServiceAbstract,
    OptionsHelperService as OptionsHelperServiceAbstract,
    Toast as ToastAbstract,
    TranslationsModule,
} from 'ngx-edu-sharing-ui';
import { CustomGlobalExtensionsComponent } from 'src/app/extension/custom-global-component/custom-global-extensions.component';
import { environment } from '../environments/environment';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CoreModule } from './core-module/core.module';
import { CoreUiModule } from './core-ui-module/core-ui.module';
import { ErrorHandlerService } from './core-ui-module/error-handler.service';
import { OptionsHelperService } from './core-ui-module/options-helper.service';
import { Toast } from './core-ui-module/toast';
import { extensionDeclarations } from './extension/extension-declarations';
import { extensionImports } from './extension/extension-imports';
import { extensionProviders } from './extension/extension-providers';
import { extensionSchemas } from './extension/extension-schemas';
import { DialogsModule } from './features/dialogs/dialogs.module';
import { MdsModule } from './features/mds/mds.module';
import { AppLocationStrategy } from './main/location-strategy';
import { MainModule } from './main/main.module';
import { DECLARATIONS_MANAGEMENT_DIALOGS } from './modules/management-dialogs/declarations';
import { CordovaService } from './services/cordova.service';
import { KeyboardShortcutsService } from './services/keyboard-shortcuts.service';
import { SharedModule } from './shared/shared.module';

// http://blog.angular-university.io/angular2-ngmodule/
// -> Making modules more readable using the spread operator

const matTooltipDefaultOptions: MatTooltipDefaultOptions = {
    showDelay: 500,
    hideDelay: 0,
    touchendHideDelay: 0,
};

@NgModule({
    declarations: [
        AppComponent,
        CustomGlobalExtensionsComponent,
        DECLARATIONS_MANAGEMENT_DIALOGS,
        extensionDeclarations,
    ],
    imports: [
        ApolloModule,
        AppRoutingModule,
        BrowserAnimationsModule,
        BrowserModule,
        CoreModule,
        CoreUiModule,
        CoreUiModule,
        DialogsModule,
        DragDropModule,
        EduSharingApiModule.forRoot(),
        EduSharingGraphqlModule,
        EduSharingUiModule.forRoot({ production: environment.production }),
        extensionImports,
        HttpClientModule,
        MainModule,
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
                    onError: (err, req) => errorHandler.handleError(err, req),
                } as EduSharingApiConfigurationParams),
        },
        {
            provide: APOLLO_OPTIONS,
            useFactory: (httpLink: HttpLink) => {
                return {
                    link: httpLink.create({ uri: '/edu-sharing/graphql' }),
                    cache: new InMemoryCache(),
                };
            },
            deps: [HttpLink],
        },
        { provide: LocationStrategy, useClass: AppLocationStrategy },
        { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
        { provide: MAT_TOOLTIP_DEFAULT_OPTIONS, useValue: matTooltipDefaultOptions },
        extensionProviders,
        ErrorHandlerService,
    ],
    exports: [DECLARATIONS_MANAGEMENT_DIALOGS],
    schemas: [].concat(extensionSchemas),
    bootstrap: [AppComponent],
})
export class AppModule {}
