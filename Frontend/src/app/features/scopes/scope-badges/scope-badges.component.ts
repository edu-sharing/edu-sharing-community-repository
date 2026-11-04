import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Node, NodeScope, NodeWithScopes, RestConstants, ScopesService } from 'ngx-edu-sharing-api';
import { combineLatest } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { SharedModule } from '../../../shared/shared.module';
import { Toast } from '../../../services/toast';
import { ScopeEditorialService } from '../scope-editorial.service';
import { ScopeInventoryComponent } from '../scope-inventory/scope-inventory.component';
import { ScopeMdsService, ScopeWidgets } from '../scope-mds.service';

type Badge = { scope: string; label: string; value: string };

/**
 * Scope information of a node: the inventory decision (directly selectable) and a compact status of
 * the own active scope (editors) and the public properties of published entries of other scopes,
 * e.g. "WLO: highly recommended".
 */
@Component({
    selector: 'es-scope-badges',
    templateUrl: 'scope-badges.component.html',
    styleUrls: ['scope-badges.component.scss'],
    imports: [SharedModule, ScopeInventoryComponent],
})
export class ScopeBadgesComponent {
    private scopesService = inject(ScopesService);
    private scopeMdsService = inject(ScopeMdsService);
    private scopeEditorialService = inject(ScopeEditorialService);
    private toast = inject(Toast);

    readonly node = input<Node>();

    protected readonly inventoryProperty = RestConstants.CCM_PROP_EDITORIAL_INVENTORY;
    /** entry after a change made here (the node dto is not reloaded) */
    private readonly saved = signal<NodeScope | null>(null);
    protected readonly saving = signal(false);

    /** default mds plus the scope mds (overrides), used for captions of all scopes */
    protected readonly widgets = toSignal(
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
        const entry = this.saved() ?? this.scopesService.getNodeScope(node);
        const widgets = this.widgets();
        const fields = this.scopesService.fields();
        const inventory = fields.some((f) => f.id === this.inventoryProperty);
        const status = fields
            .filter(
                (f) =>
                    !f.replaces &&
                    f.id !== this.inventoryProperty &&
                    entry?.properties?.[f.id]?.length,
            )
            .map((f) => ({
                label: ScopeMdsService.getLabel(widgets, f.id),
                value: entry.properties[f.id]
                    .map((v) => ScopeMdsService.getValueLabel(widgets, f.id, v))
                    .join(', '),
            }));
        return {
            entry,
            status,
            inventory: inventory
                ? { value: entry?.properties?.[this.inventoryProperty]?.[0] }
                : null,
        };
    });

    constructor() {
        effect(() => {
            this.node();
            this.saved.set(null);
        });
        // changed here or in the editorial panel: show the new state (published, values)
        effect(() => {
            const change = this.scopeEditorialService.entryChanged();
            untracked(() => {
                const node = this.node();
                if (!change || !node || change.nodeId !== node.ref?.id) {
                    return;
                }
                this.saved.set(
                    (
                        ScopeEditorialService.applyEntryChange(
                            node,
                            change,
                            this.scopesService.fields(),
                        ) as NodeWithScopes
                    ).scopes.find((s) => s.scopeId === change.scopeId) ?? null,
                );
            });
        });
    }

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

    /** null: back to the inbox (no decision) */
    protected setInventory(value: string | null) {
        const scopeId = this.scopesService.activeScope()?.scopeId;
        const node = this.node();
        if (!scopeId || !node) {
            return;
        }
        this.saving.set(true);
        this.scopesService
            .setAssignment(scopeId, node.ref.id, { [this.inventoryProperty]: value ? [value] : [] })
            .subscribe({
                next: (assignment) => {
                    this.saving.set(false);
                    this.scopeEditorialService.notifyEntryChanged({
                        scopeId,
                        nodeId: node.ref.id,
                        assignment,
                    });
                    this.toast.toast('TOAST.SCOPE.SAVED');
                },
                error: (error) => {
                    this.saving.set(false);
                    this.toast.error(error);
                },
            });
    }
}
