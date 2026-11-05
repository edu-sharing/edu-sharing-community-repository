import { Component, input, InputSignal, output } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { DEFAULT_PAGE_VARIANT_NAME_PREFIX } from '../../shared/types/custom-definitions';

/**
 * Selects the variant of a topic page; editors of the collection can also switch to its template.
 */
@Component({
    selector: 'es-topic-page-variant-select',
    imports: [EduSharingUiCommonModule, MatFormFieldModule, MatSelectModule, TranslateModule],
    templateUrl: './variant-select.component.html',
    styleUrls: ['./variant-select.component.scss'],
})
export class VariantSelectComponent {
    readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.VARIANT_SELECT.';
    readonly navigateToTemplateValue: string = 'NAVIGATE_TO_TEMPLATE';
    protected readonly pageVariantConfigPrefix: string = DEFAULT_PAGE_VARIANT_NAME_PREFIX;

    // whether the user may open the page template of the collection
    canManagePageTemplate: InputSignal<boolean> = input<boolean>(false);
    isProcessing: InputSignal<boolean> = input<boolean>(false);
    pageTemplateExists: InputSignal<boolean> = input<boolean>(false);
    pageVariantConfigNodes: InputSignal<Node[]> = input<Node[]>([]);
    pageVariantNode: InputSignal<Node | null> = input<Node | null>(null);
    templateMode: InputSignal<boolean> = input<boolean>(false);
    readonly navigateToTemplate = output<void>();
    readonly variantSelected = output<string>();

    selectVariant(variantId: string): void {
        if (variantId === this.navigateToTemplateValue) {
            this.navigateToTemplate.emit();
            return;
        }
        this.variantSelected.emit(variantId);
    }
}
