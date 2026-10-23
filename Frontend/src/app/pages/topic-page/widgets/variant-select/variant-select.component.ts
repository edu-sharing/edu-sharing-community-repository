import {
    Component,
    inject,
    input,
    InputSignal,
    output,
    signal,
    WritableSignal,
} from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { RestConnectorService } from '../../../../core-module/rest/services/rest-connector.service';
import { DEFAULT_PAGE_VARIANT_NAME_PREFIX } from '../../shared/types/custom-definitions';

/**
 * Selects the variant of a topic page; admins can also switch to the page template it inherits.
 */
@Component({
    selector: 'es-topic-page-variant-select',
    imports: [EduSharingUiCommonModule, MatFormFieldModule, MatSelectModule, TranslateModule],
    templateUrl: './variant-select.component.html',
    styleUrls: ['./variant-select.component.scss'],
})
export class VariantSelectComponent {
    private connector = inject(RestConnectorService);

    readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.VARIANT_SELECT.';
    readonly navigateToTemplateValue: string = 'NAVIGATE_TO_TEMPLATE';
    protected readonly pageVariantConfigPrefix: string = DEFAULT_PAGE_VARIANT_NAME_PREFIX;

    isProcessing: InputSignal<boolean> = input<boolean>(false);
    pageTemplateExists: InputSignal<boolean> = input<boolean>(false);
    pageVariantConfigNodes: InputSignal<Node[]> = input<Node[]>([]);
    pageVariantNode: InputSignal<Node | null> = input<Node | null>(null);
    templateMode: InputSignal<boolean> = input<boolean>(false);
    readonly navigateToTemplate = output<void>();
    readonly variantSelected = output<string>();

    isAdmin: WritableSignal<boolean> = signal(false);

    constructor() {
        this.connector.isLoggedIn().subscribe((): void => {
            this.isAdmin.set(this.connector.getCurrentLogin()?.isAdmin ?? false);
        });
    }

    selectVariant(variantId: string): void {
        if (variantId === this.navigateToTemplateValue) {
            this.navigateToTemplate.emit();
            return;
        }
        this.variantSelected.emit(variantId);
    }
}
