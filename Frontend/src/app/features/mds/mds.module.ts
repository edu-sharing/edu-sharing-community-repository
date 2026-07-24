import { NgxSliderModule } from '@angular-slider/ngx-slider';
import { NgModule } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { EditorModule } from '@tinymce/tinymce-angular';
import { SharedModule } from '../../shared/shared.module';
import { MdsEditorCoreComponent } from './mds-editor/mds-editor-core/mds-editor-core.component';
import { MdsEditorEmbeddedComponent } from './mds-editor/mds-editor-embedded/mds-editor-embedded.component';
import { MdsEditorViewComponent } from './mds-editor/mds-editor-view/mds-editor-view.component';
import { MdsEditorWrapperComponent } from './mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { MdsEditorComponent } from './mds-editor/mds-editor.component';
import { SearchFieldFacetsComponent } from './mds-editor/search-field-facets/search-field-facets.component';
import { LabelPipe } from './mds-editor/shared/label.pipe';
import { MdsEditorWidgetAuthorComponent } from './mds-editor/widgets/mds-editor-widget-author/mds-editor-widget-author.component';
import { MdsEditorWidgetAuthorityComponent } from './mds-editor/widgets/mds-editor-widget-authority/mds-editor-widget-authority.component';
import { MdsEditorWidgetCheckboxComponent } from './mds-editor/widgets/mds-editor-widget-checkbox/mds-editor-widget-checkbox.component';
import { MdsEditorWidgetCheckboxesComponent } from './mds-editor/widgets/mds-editor-widget-checkboxes/mds-editor-widget-checkboxes.component';
import { MdsEditorWidgetCollectionsComponent } from './mds-editor/widgets/mds-editor-widget-collections/mds-editor-widget-collections.component';
import {
    MdsEditorWidgetChipsComponent,
    MdsEditorWidgetChipsRangedValueComponent,
} from './mds-editor/widgets/mds-editor-widget-chips/mds-editor-widget-chips.component';
import { MdsEditorWidgetContainerComponent } from './mds-editor/widgets/mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from './mds-editor/widgets/mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetDurationComponent } from './mds-editor/widgets/mds-editor-widget-duration/mds-editor-widget-duration.component';
import { MdsEditorWidgetErrorComponent } from './mds-editor/widgets/mds-editor-widget-error/mds-editor-widget-error.component';
import { MdsEditorWidgetFacetListComponent } from './mds-editor/widgets/mds-editor-widget-facet-list/mds-editor-widget-facet-list.component';
import { MdsEditorWidgetFileUploadComponent } from './mds-editor/widgets/mds-editor-widget-file-upload/mds-editor-widget-file-upload.component';
import { LicenseDetailsComponent } from './mds-editor/widgets/mds-editor-widget-license/license-details/license-details.component';
import { MdsEditorWidgetLicenseComponent } from './mds-editor/widgets/mds-editor-widget-license/mds-editor-widget-license.component';
import { MdsEditorWidgetLinkComponent } from './mds-editor/widgets/mds-editor-widget-link/mds-editor-widget-link.component';
import { AiPreviewImagesOverlayComponent } from './mds-editor/widgets/mds-editor-widget-preview/ai-preview-images-overlay/ai-preview-images-overlay.component';
import { MdsEditorWidgetPreviewComponent } from './mds-editor/widgets/mds-editor-widget-preview/mds-editor-widget-preview.component';
import { MdsEditorWidgetRadioButtonComponent } from './mds-editor/widgets/mds-editor-widget-radio-button/mds-editor-widget-radio-button.component';
import { MdsEditorWidgetSearchSuggestionsComponent } from './mds-editor/widgets/mds-editor-widget-search-suggestions/mds-editor-widget-search-suggestions.component';
import { MdsEditorWidgetSelectComponent } from './mds-editor/widgets/mds-editor-widget-select/mds-editor-widget-select.component';
import {
    MdsEditorWidgetSliderComponent,
    MdsEditorWidgetSliderRangeComponent,
} from './mds-editor/widgets/mds-editor-widget-slider/mds-editor-widget-slider.component';
import { MdsEditorWidgetSuggestionChipsComponent } from './mds-editor/widgets/mds-editor-widget-suggestion-chips/mds-editor-widget-suggestion-chips.component';
import { MdsEditorWidgetTextComponent } from './mds-editor/widgets/mds-editor-widget-text/mds-editor-widget-text.component';
import { HighlightPipe } from './mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree-core/highlight.pipe';
import { MdsEditorWidgetTreeCoreComponent } from './mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { MdsEditorWidgetTreeValueSelectionComponent } from './mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree-value-section/mds-editor-widget-tree-value-selection.component';
import {
    EsProposalChipDirective,
    MdsEditorWidgetTreeComponent,
} from './mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree.component';
import { MdsEditorWidgetVCardComponent } from './mds-editor/widgets/mds-editor-widget-vcard/mds-editor-widget-vcard.component';
import { MdsEditorWidgetVersionComponent } from './mds-editor/widgets/mds-editor-widget-version/mds-editor-widget-version.component';
import { MdsEditorWidgetTinyMCEComponent } from './mds-editor/widgets/mds-editor-widget-wysiwyg-html/mds-editor-widget-tinymce.component';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { MdsEditorWidgetChildobjectsComponent } from './mds-editor/widgets/mds-editor-widget-childobjects/mds-editor-widget-childobjects.component';
import { LicenseAiPipe } from './mds-editor/widgets/mds-editor-widget-license/license-details/license-ai.pipe';
import { MdsEditorSingleWidgetComponent } from './mds-editor/mds-editor-single-widget/mds-editor-single-widget.component';
import { MdsEditorWidgetCommentsComponent } from './mds-editor/widgets/mds-editor-widget-comments/mds-editor-widget-comments.component';
import { CommentsListComponent } from './mds-editor/widgets/mds-editor-widget-comments/comments-list/comments-list.component';
import { MdsNodeRelationsWidgetComponent } from './mds-editor/widgets/mds-editor-widget-relations/node-relations/node-relations-widget.component';
import { MdsEditorWidgetRelationsComponent } from './mds-editor/widgets/mds-editor-widget-relations/mds-editor-widget-relations.component';
import { MdsEditorWidgetNodefilterComponent } from './mds-editor/widgets/mds-editor-widget-nodefilter/mds-editor-widget-nodefilter.component';
import { MdsEditorWidgetMultivalueButtonsComponent } from './mds-editor/widgets/mds-editor-widget-multivalue-buttons/mds-editor-widget-multivalue-buttons.component';

@NgModule({
    declarations: [
        AiPreviewImagesOverlayComponent,
        HighlightPipe,
        LabelPipe,
        LicenseAiPipe,
        LicenseDetailsComponent,
        MdsEditorComponent,
        MdsEditorCoreComponent,
        MdsEditorEmbeddedComponent,
        MdsEditorViewComponent,
        MdsEditorWidgetAuthorComponent,
        MdsEditorWidgetNodefilterComponent,
        MdsEditorWidgetCollectionsComponent,
        MdsEditorWidgetRelationsComponent,
        MdsEditorWidgetCommentsComponent,
        MdsEditorWidgetAuthorityComponent,
        MdsEditorWidgetCheckboxComponent,
        MdsEditorWidgetCheckboxesComponent,
        MdsEditorWidgetChildobjectsComponent,
        MdsEditorWidgetMultivalueButtonsComponent,
        MdsEditorWidgetChipsComponent,
        MdsEditorWidgetChipsRangedValueComponent,
        MdsEditorWidgetContainerComponent,
        MdsEditorWidgetDurationComponent,
        MdsEditorWidgetErrorComponent,
        MdsEditorWidgetFacetListComponent,
        MdsEditorWidgetFileUploadComponent,
        MdsEditorWidgetLicenseComponent,
        MdsEditorWidgetLinkComponent,
        MdsEditorWidgetPreviewComponent,
        MdsEditorWidgetRadioButtonComponent,
        MdsEditorWidgetSearchSuggestionsComponent,
        MdsEditorWidgetSelectComponent,
        MdsEditorWidgetSliderComponent,
        MdsEditorWidgetSliderRangeComponent,
        MdsEditorWidgetSuggestionChipsComponent,
        MdsEditorWidgetTextComponent,
        MdsEditorWidgetTinyMCEComponent,
        MdsEditorWidgetTreeComponent,
        MdsEditorWidgetTreeCoreComponent,
        EsProposalChipDirective,
        MdsEditorWidgetVCardComponent,
        MdsEditorWidgetVersionComponent,
        MdsEditorWrapperComponent,
        MdsEditorSingleWidgetComponent,
        RegisterFormFieldDirective,
        SearchFieldFacetsComponent,
    ],
    imports: [
        SharedModule,
        NgxSliderModule,
        EditorModule,
        MatFormFieldModule,
        EduSharingUiModule,
        CommentsListComponent,
        MdsEditorWidgetTreeValueSelectionComponent,
        MdsNodeRelationsWidgetComponent,
    ],
    exports: [
        AiPreviewImagesOverlayComponent,
        LicenseDetailsComponent,
        LicenseAiPipe,
        MdsEditorWidgetPreviewComponent,
        MdsEditorWidgetAuthorComponent,
        MdsEditorWidgetCollectionsComponent,
        MdsEditorWidgetRelationsComponent,
        MdsEditorWidgetCommentsComponent,
        MdsEditorWrapperComponent,
        MdsEditorCoreComponent,
        MdsEditorSingleWidgetComponent,
        SearchFieldFacetsComponent,
        MdsEditorWidgetContainerComponent,
    ],
})
export class MdsModule {}
