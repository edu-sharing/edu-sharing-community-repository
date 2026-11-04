import { Component, computed, inject, input, output } from '@angular/core';
import { MatRadioModule } from '@angular/material/radio';
import { RestConstants, ScopesService } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../shared/shared.module';
import { ScopeMdsService, ScopeWidgets } from '../scope-mds.service';

let nextId = 0;

/**
 * The inventory decision (`ccm:editorial_inventory`) of a scope entry as a one click choice of its
 * valuespace (captions and icons of the scope mds) and the resulting state (published / excluded,
 * derived from the field of the active scope). Used by the editorial panel and the preview, so both
 * show the same state for the current selection.
 */
@Component({
    selector: 'es-scope-inventory',
    templateUrl: 'scope-inventory.component.html',
    styleUrls: ['scope-inventory.component.scss'],
    imports: [SharedModule, MatRadioModule],
})
export class ScopeInventoryComponent {
    private scopesService = inject(ScopesService);

    /** widgets of the scope mds (caption, values and icons of the inventory property) */
    readonly widgets = input<ScopeWidgets>();
    /** current decision, null / undefined if there is none yet */
    readonly value = input<string | null>();
    /** bulk: the selected contents have different decisions (no radio selected) */
    readonly mixed = input(false);
    readonly disabled = input(false);
    /** the new decision, null for "inbox" (no decision) */
    readonly valueChange = output<string | null>();

    protected readonly labelId = 'scope-inventory-' + nextId++;
    protected readonly property = RestConstants.CCM_PROP_EDITORIAL_INVENTORY;
    protected readonly widget = computed(() => this.widgets()?.[this.property]);
    /** state of the selected value, same rule as the backend (publishedValues / excludedValues) */
    protected readonly state = computed(() => {
        const field = this.scopesService.getField(this.property);
        const value = this.value();
        return {
            published: !!value && !!field?.publishedValues?.includes(value),
            excluded: !!value && !!field?.excludedValues?.includes(value),
        };
    });
    /** radio value for "no decision yet" (unset) */
    protected readonly NONE = ScopeMdsService.INVENTORY_NONE;

    protected onChange(value: string) {
        this.valueChange.emit(value === this.NONE ? null : value);
    }
}
