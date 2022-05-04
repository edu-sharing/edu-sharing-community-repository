import {NgModule} from '@angular/core';
import {DECLARATIONS} from './declarations';
import {IMPORTS} from './imports';
import {PROVIDERS} from './providers';
import {RouterComponent} from './router/router.component';
import {DECLARATIONS_RECYCLE} from './modules/node-list/declarations';
import {DECLARATIONS_WORKSPACE} from './modules/workspace/declarations';
import {DECLARATIONS_SEARCH} from './modules/search/declarations';
import {PROVIDERS_SEARCH} from './modules/search/providers';
import {DECLARATIONS_COLLECTIONS} from './modules/collections/declarations';
import {DECLARATIONS_LOGIN} from './modules/login/declarations';
import {DECLARATIONS_LOGINAPP} from './modules/login-app/declarations';
import {DECLARATIONS_PERMISSIONS} from './modules/permissions/declarations';
import {DECLARATIONS_OER} from './modules/oer/declarations';
import {DECLARATIONS_ADMIN} from './modules/admin/declarations';
import {DECLARATIONS_MANAGEMENT_DIALOGS} from './modules/management-dialogs/declarations';
import {DECLARATIONS_MESSAGES} from './modules/messages/declarations';
import {DECLARATIONS_STREAM} from './modules/stream/declarations';
import {DECLARATIONS_PROFILES} from './modules/profiles/declarations';
import {DECLARATIONS_STARTUP} from './modules/startup/declarations';
import {DECLARATIONS_SHARE_APP} from './modules/share-app/declarations';
import {DECLARATIONS_SHARING} from './modules/sharing/declarations';
import {DECLARATIONS_REGISTER} from './modules/register/declarations';
import {DECLARATIONS_SERVICES} from "./modules/services/declarations";
import {DECLARATIONS_FILE_UPLOAD} from './modules/file-upload/declarations';
import {CommentsListComponent} from "./modules/management-dialogs/node-comments/comments-list/comments-list.component";
import {MdsWidgetComponent} from "./common/ui/mds-viewer/widget/mds-widget.component";
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from "@angular/material/form-field";
import { MAT_TOOLTIP_DEFAULT_OPTIONS } from "@angular/material/tooltip";
import {ButtonsTestComponent} from './common/test/buttons/buttons-test.component';
import {InputsTestComponent} from './common/test/inputs/inputs-test.component';
import {UserAvatarTestComponent} from './common/test/user-avatar/user-avatar-test.component';
import {ModalTestComponent} from './common/test/modal/modal-test.component';
import { MainMenuSidebarComponent } from './common/ui/main-menu-sidebar/main-menu-sidebar.component';
import { MainMenuBottomComponent } from './common/ui/main-menu-bottom/main-menu-bottom.component';
import { MainMenuDropdownComponent } from './common/ui/main-menu-dropdown/main-menu-dropdown.component';
import { LuceneTemplateMemoryComponent } from './modules/admin/lucene-template-memory/lucene-template-memory.component';
import { MdsEditorWrapperComponent } from './common/ui/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { MdsEditorCardComponent } from './common/ui/mds-editor/mds-editor-card/mds-editor-card.component';
import { MdsEditorCoreComponent } from './common/ui/mds-editor/mds-editor-core/mds-editor-core.component';
import { MdsEditorViewComponent } from './common/ui/mds-editor/mds-editor-view/mds-editor-view.component';
import { MdsEditorWidgetTextComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-text/mds-editor-widget-text.component';
import { MdsEditorWidgetContainerComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-container/mds-editor-widget-container.component';
import { MdsEditorWidgetPreviewComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-preview/mds-editor-widget-preview.component';
import { MdsEditorWidgetErrorComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-error/mds-editor-widget-error.component';
import { MdsEditorWidgetChipsComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-chips/mds-editor-widget-chips.component';
import { MdsEditorWidgetVersionComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-version/mds-editor-widget-version.component';
import { MdsEditorWidgetLinkComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-link/mds-editor-widget-link.component';
import {MdsEditorWidgetSelectComponent} from './common/ui/mds-editor/widgets/mds-editor-widget-select/mds-editor-widget-select.component';
import {MdsEditorWidgetSliderComponent} from './common/ui/mds-editor/widgets/mds-editor-widget-slider/mds-editor-widget-slider.component';
import { NgxSliderModule } from '@angular-slider/ngx-slider';
import { MdsEditorWidgetTreeComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree.component';
import { MdsEditorWidgetTreeCoreComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import {MdsEditorWidgetAuthorComponent} from './common/ui/mds-editor/widgets/mds-editor-widget-author/mds-editor-widget-author.component';
import { HighlightPipe } from './common/ui/mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree-core/highlight.pipe';
import {MdsEditorWidgetChildobjectsComponent} from './common/ui/mds-editor/widgets/mds-editor-widget-childobjects/mds-editor-widget-childobjects.component';
import {DragDropModule} from '@angular/cdk/drag-drop';
import { MdsEditorWidgetCheckboxComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-checkbox/mds-editor-widget-checkbox.component';
import { MdsEditorWidgetRadioButtonComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-radio-button/mds-editor-widget-radio-button.component';
import { MdsEditorWidgetCheckboxesComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-checkboxes/mds-editor-widget-checkboxes.component';
import {MdsEditorWidgetDurationComponent} from './common/ui/mds-editor/widgets/mds-editor-widget-duration/mds-editor-widget-duration.component';
import {MdsEditorWidgetLicenseComponent} from './common/ui/mds-editor/widgets/mds-editor-widget-license/mds-editor-widget-license.component';
import { MdsEditorEmbeddedComponent } from './common/ui/mds-editor/mds-editor-embedded/mds-editor-embedded.component';
import { MdsEditorWidgetSuggestionChipsComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-suggestion-chips/mds-editor-widget-suggestion-chips.component';
import {MdsEditorWidgetFileUploadComponent} from './common/ui/mds-editor/widgets/mds-editor-widget-file-upload/mds-editor-widget-file-upload.component';
import {CommonModule} from '@angular/common';
import { MultiLineLabelComponent } from './common/ui/multi-line-label/multi-line-label.component';
import { CheckTextOverflowDirective } from './core-ui-module/directives/check-text-overflow.directive';
import { RegisterCustomPropertyDirective } from './common/directives/register-custom-property.directive';
import { OnAttributeChangeDirective } from './common/directives/on-attribute-change.directive';
import { MdsEditorWidgetAuthorityComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-authority/mds-editor-widget-authority.component';
import { extensionDeclarations } from './extension/extension-declarations';
import { extensionImports } from './extension/extension-imports';
import {ResizableModule} from 'angular-resizable-element';
import { MainMenuButtonsComponent } from './common/ui/main-menu-buttons/main-menu-buttons.component';
import { MdsEditorWidgetFacetListComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-facet-list/mds-editor-widget-facet-list.component';
import { SearchFieldComponent } from './common/ui/search-field/search-field.component';
import { MdsEditorComponent } from './common/ui/mds-editor/mds-editor.component';
import { SearchFieldFacetsComponent } from './common/ui/mds-editor/search-field-facets/search-field-facets.component';
import { LabelPipe } from './common/ui/mds-editor/shared/label.pipe';
import { PropertySlugPipe } from './common/ui/mds-editor/shared/property-slug.pipe';
import { MdsEditorWidgetSearchSuggestionsComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-search-suggestions/mds-editor-widget-search-suggestions.component';
import { EduSharingApiModule, EDU_SHARING_API_CONFIG } from 'ngx-edu-sharing-api';
import { EduSharingGraphqlModule } from 'ngx-edu-sharing-graphql';
import { MdsEditorWidgetVCardComponent } from './common/ui/mds-editor/widgets/mds-editor-widget-vcard/mds-editor-widget-vcard.component';
import { extensionProviders } from './extension/extension-providers';
import {
    MdsEditorWidgetTinyMCE
} from './common/ui/mds-editor/widgets/mds-editor-widget-wysiwyg-html/mds-editor-widget-tinymce.component';
import {EditorModule} from '@tinymce/tinymce-angular';
import { LtiComponent } from './modules/lti/lti.component';
import { LtiAdminComponent } from './modules/admin/lti-admin/lti-admin.component';
import { NodeEmbedComponent } from './common/ui/node-embed/node-embed.component';
import {EduSharingApiConfigurationParams} from 'ngx-edu-sharing-api';
import {ErrorHandlerService} from './core-ui-module/error-handler.service';
import { Toast } from './core-ui-module/toast';
import { TranslationsModule } from './translations/translations.module';
import { MainModule } from './main/main.module';
import {ApolloClientOptions, InMemoryCache} from '@apollo/client';
import {HttpLink} from 'apollo-angular/http';
import {APOLLO_OPTIONS, ApolloModule} from 'apollo-angular';


// http://blog.angular-university.io/angular2-ngmodule/
// -> Making modules more readable using the spread operator

@NgModule({
    declarations: [
        DECLARATIONS,
        DECLARATIONS_RECYCLE,
        DECLARATIONS_WORKSPACE,
        DECLARATIONS_SEARCH,
        DECLARATIONS_COLLECTIONS,
        DECLARATIONS_LOGIN,
        DECLARATIONS_REGISTER,
        DECLARATIONS_LOGINAPP,
        DECLARATIONS_FILE_UPLOAD,
        DECLARATIONS_STARTUP,
        DECLARATIONS_PERMISSIONS,
        DECLARATIONS_OER,
        DECLARATIONS_STREAM,
        DECLARATIONS_MANAGEMENT_DIALOGS,
        DECLARATIONS_ADMIN,
        DECLARATIONS_PROFILES,
        DECLARATIONS_MESSAGES,
        DECLARATIONS_SHARING,
        DECLARATIONS_SHARE_APP,
        DECLARATIONS_SERVICES,
        MainMenuSidebarComponent,
        MainMenuBottomComponent,
        MainMenuDropdownComponent,
        LuceneTemplateMemoryComponent,
        MdsEditorWrapperComponent,
        MdsEditorCardComponent,
        MdsEditorCoreComponent,
        MdsEditorViewComponent,
        MdsEditorWidgetTextComponent,
        MdsEditorWidgetContainerComponent,
        MdsEditorWidgetPreviewComponent,
        MdsEditorWidgetAuthorComponent,
        MdsEditorWidgetVCardComponent,
        MdsEditorWidgetAuthorityComponent,
        MdsEditorWidgetChildobjectsComponent,
        MdsEditorWidgetErrorComponent,
        MdsEditorWidgetChipsComponent,
        MdsEditorWidgetSelectComponent,
        MdsEditorWidgetSliderComponent,
        MdsEditorWidgetDurationComponent,
        MdsEditorWidgetVersionComponent,
        MdsEditorWidgetLinkComponent,
        MdsEditorWidgetLicenseComponent,
        MdsEditorWidgetFileUploadComponent,
        MdsEditorWidgetTreeComponent,
        MdsEditorWidgetTreeCoreComponent,
        HighlightPipe,
        MdsEditorWidgetCheckboxComponent,
        MdsEditorWidgetTinyMCE,
        MdsEditorWidgetRadioButtonComponent,
        MdsEditorWidgetCheckboxesComponent,
        MdsEditorEmbeddedComponent,
        MdsEditorWidgetSuggestionChipsComponent,
        MultiLineLabelComponent,
        RegisterCustomPropertyDirective,
        OnAttributeChangeDirective,
        extensionDeclarations,
        MainMenuButtonsComponent,
        MdsEditorWidgetFacetListComponent,
        SearchFieldComponent,
        MdsEditorComponent,
        SearchFieldFacetsComponent,
        LabelPipe,
        PropertySlugPipe,
        MdsEditorWidgetSearchSuggestionsComponent,
        LtiComponent,
        LtiAdminComponent,
        NodeEmbedComponent,
    ],
    imports: [
        IMPORTS,
        ApolloModule,
        CommonModule,
        MainModule,
        EduSharingApiModule.forRoot(),
        EduSharingGraphqlModule,
        TranslationsModule.forRoot(),
        NgxSliderModule,
        DragDropModule,
        extensionImports,
        ResizableModule,
        EditorModule,
    ],
    providers: [
        {
            provide: EDU_SHARING_API_CONFIG,
            deps: [ErrorHandlerService],
            useFactory: (errorHandler: ErrorHandlerService) => ({
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
        PROVIDERS,
        PROVIDERS_SEARCH,
        {provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: {appearance: 'outline'}},
        {provide: MAT_TOOLTIP_DEFAULT_OPTIONS, useValue: {showDelay: 500}},
        extensionProviders,
        ErrorHandlerService,
    ],
    exports: [
        DECLARATIONS,
        DECLARATIONS_RECYCLE,
        DECLARATIONS_WORKSPACE,
        DECLARATIONS_SEARCH,
        DECLARATIONS_COLLECTIONS,
        DECLARATIONS_LOGIN,
        DECLARATIONS_REGISTER,
        DECLARATIONS_LOGINAPP,
        DECLARATIONS_FILE_UPLOAD,
        DECLARATIONS_STARTUP,
        DECLARATIONS_PERMISSIONS,
        DECLARATIONS_OER,
        DECLARATIONS_STREAM,
        DECLARATIONS_MANAGEMENT_DIALOGS,
        DECLARATIONS_ADMIN,
        DECLARATIONS_PROFILES,
        DECLARATIONS_MESSAGES,
        DECLARATIONS_SHARING,
        DECLARATIONS_SHARE_APP,
        DECLARATIONS_SERVICES
    ],
    bootstrap: [RouterComponent]
})
export class AppModule { }
