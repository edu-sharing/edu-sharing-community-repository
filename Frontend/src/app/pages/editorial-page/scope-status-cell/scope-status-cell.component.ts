import { Component, computed, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { TranslateModule } from '@ngx-translate/core';
import { Node, RestConstants, ScopesService } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { switchMap } from 'rxjs/operators';
import { ScopeEditorialService } from '../../../features/scopes/scope-editorial.service';
import { ScopeMdsService } from '../../../features/scopes/scope-mds.service';

/**
 * Table cell of a field of the active scope: the effective value (valuespace values as chips, free
 * values as text, labeled via the scope mds). For a replacing field, whose value the scope has
 * replaced, the original value follows in a second line.
 */
@Component({
    selector: 'es-scope-status-cell',
    templateUrl: 'scope-status-cell.component.html',
    styleUrls: ['scope-status-cell.component.scss'],
    imports: [EduSharingUiCommonModule, TranslateModule],
})
export class ScopeStatusCellComponent {
    private scopesService = inject(ScopesService);
    private scopeMdsService = inject(ScopeMdsService);
    private scopeEditorialService = inject(ScopeEditorialService);

    readonly node = input.required<Node>();
    /** the node is patched in place after an entry was changed (see EditorialPageComponent) */
    private readonly entry = computed(
        () => {
            this.scopeEditorialService.entryChanged();
            return { node: this.node(), scope: this.scopesService.getNodeScope(this.node()) };
        },
        { equal: () => false },
    );
    readonly property = input.required<string>();

    private readonly widgets = toSignal(
        toObservable(computed(() => this.scopesService.activeScope()?.definition?.mds)).pipe(
            switchMap((mds) => this.scopeMdsService.observeWidgets(mds)),
        ),
    );
    private readonly field = computed(() =>
        this.scopesService.fields().find((f) => f.id === this.property()),
    );

    /** values of a valuespace are shown as chips, free values as text */
    readonly chips = computed(() => !!this.widgets()?.[this.property()]?.values?.length);
    readonly values = computed(() => {
        const property = this.property();
        // a replacing field: the node dto already carries the effective value inside the scope
        const { node, scope } = this.entry();
        const raw = this.field()?.replaces
            ? node?.properties?.[property]
            : scope?.properties?.[property];
        const values = this.display(raw);
        // no inventory decision yet: shown as the "inbox" value of its valuespace
        if (!values.length && property === RestConstants.CCM_PROP_EDITORIAL_INVENTORY) {
            return this.display([ScopeMdsService.INVENTORY_NONE]);
        }
        return values;
    });
    /** labels of the effective values (text) */
    readonly labels = computed(() => this.values().map((v) => v.label));
    /** original value, only if the scope replaced it (editors only, see NodeScope.originals) */
    readonly originals = computed(() =>
        this.field()?.replaces ? this.label(this.entry().scope?.originals?.[this.property()]) : [],
    );
    readonly caption = computed(() => ScopeMdsService.getLabel(this.widgets(), this.property()));

    private label(values: string[] | undefined): string[] {
        return this.display(values).map((v) => v.label);
    }

    /** label and icon (of the mds valuespace) of each value */
    private display(values: string[] | undefined): { label: string; icon?: string }[] {
        return (values ?? [])
            .filter((value) => value != null && value !== '')
            .map((value) => {
                const mdsValue = ScopeMdsService.getValue(this.widgets(), this.property(), value);
                return { label: mdsValue?.caption || value, icon: mdsValue?.icon };
            });
    }
}
