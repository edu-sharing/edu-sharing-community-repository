import { Component, computed, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Node, ScopesService } from 'ngx-edu-sharing-api';
import { combineLatest } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { SharedModule } from '../../../shared/shared.module';
import { ScopeMdsService, ScopeWidgets } from '../scope-mds.service';

type Badge = { scope: string; label: string; value: string };

/**
 * Scope information of a node: a compact status of the own active scope (editors) and the public
 * properties of published entries of other scopes, e.g. "WLO: highly recommended".
 */
@Component({
    selector: 'es-scope-badges',
    templateUrl: 'scope-badges.component.html',
    styleUrls: ['scope-badges.component.scss'],
    imports: [SharedModule],
})
export class ScopeBadgesComponent {
    private scopesService = inject(ScopesService);
    private scopeMdsService = inject(ScopeMdsService);

    readonly node = input<Node>();

    /** default mds plus the scope mds (overrides), used for captions of all scopes */
    private readonly widgets = toSignal(
        toObservable(computed(() => this.scopesService.activeScope()?.definition?.mds)).pipe(
            switchMap((mds) =>
                combineLatest([
                    this.scopeMdsService.observeWidgets(null),
                    this.scopeMdsService.observeWidgets(mds),
                ]),
            ),
            map(([defaults, scope]) => ({ ...defaults, ...scope } as ScopeWidgets)),
        ),
        { initialValue: {} as ScopeWidgets },
    );

    protected readonly own = computed(() => {
        const node = this.node();
        if (!node || !this.scopesService.isEditor()) {
            return null;
        }
        const entry = this.scopesService.getNodeScope(node);
        const widgets = this.widgets();
        const status = this.scopesService
            .fields()
            .filter((f) => !f.replaces && entry?.properties?.[f.id]?.length)
            .map((f) => ({
                label: ScopeMdsService.getLabel(widgets, f.id),
                value: entry.properties[f.id]
                    .map((v) => ScopeMdsService.getValueLabel(widgets, f.id, v))
                    .join(', '),
            }));
        return { entry, status };
    });

    protected readonly foreign = computed<Badge[]>(() => {
        const widgets = this.widgets();
        return this.scopesService.getForeignScopes(this.node()).flatMap((scope) =>
            Object.entries(scope.properties ?? {}).flatMap(([property, values]) =>
                values.map((value) => ({
                    scope: scope.displayName || scope.scopeId,
                    label: ScopeMdsService.getLabel(widgets, property),
                    value: ScopeMdsService.getValueLabel(widgets, property, value),
                })),
            ),
        );
    });
}
