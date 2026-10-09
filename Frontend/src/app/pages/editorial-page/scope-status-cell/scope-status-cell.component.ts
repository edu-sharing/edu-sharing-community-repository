import { Component, computed, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Node, ScopesService } from 'ngx-edu-sharing-api';
import { switchMap } from 'rxjs/operators';
import { ScopeMdsService } from '../../../features/scopes/scope-mds.service';

/**
 * Table cell with the value(s) of a property of the node's entry in the active scope
 * (`node.scopes`), labeled via the valuespace of the scope mds.
 */
@Component({
    selector: 'es-scope-status-cell',
    templateUrl: 'scope-status-cell.component.html',
    styleUrls: ['scope-status-cell.component.scss'],
})
export class ScopeStatusCellComponent {
    private scopesService = inject(ScopesService);
    private scopeMdsService = inject(ScopeMdsService);

    readonly node = input.required<Node>();
    readonly property = input.required<string>();

    private readonly widgets = toSignal(
        toObservable(computed(() => this.scopesService.activeScope()?.definition?.mds)).pipe(
            switchMap((mds) => this.scopeMdsService.observeWidgets(mds)),
        ),
    );

    readonly values = computed(() => {
        const entry = this.scopesService.getNodeScope(this.node());
        return (entry?.properties?.[this.property()] ?? []).map((value) =>
            ScopeMdsService.getValueLabel(this.widgets(), this.property(), value),
        );
    });
    readonly caption = computed(() => ScopeMdsService.getLabel(this.widgets(), this.property()));
}
