import { Component, effect, inject, input, signal } from '@angular/core';
import { ScopeHistoryEntry, ScopesService } from 'ngx-edu-sharing-api';
import { Subscription } from 'rxjs';
import { SharedModule } from '../../../../shared/shared.module';
import { ScopeMdsService, ScopeWidgets } from '../../scope-mds.service';

/**
 * History of the entry of a node in a scope (newest first, one row per changed property).
 */
@Component({
    selector: 'es-scope-history',
    templateUrl: 'scope-history.component.html',
    styleUrls: ['scope-history.component.scss'],
    imports: [SharedModule],
})
export class ScopeHistoryComponent {
    private scopesService = inject(ScopesService);

    readonly scopeId = input.required<string>();
    readonly nodeId = input.required<string>();
    readonly widgets = input<ScopeWidgets>(null);
    /** increase to reload (e.g. after the entry was saved) */
    readonly reload = input(0);

    protected readonly entries = signal<ScopeHistoryEntry[] | null>(null);
    protected readonly error = signal(false);
    private subscription: Subscription;

    constructor() {
        effect(() => {
            const scopeId = this.scopeId();
            const nodeId = this.nodeId();
            this.reload();
            this.subscription?.unsubscribe();
            this.entries.set(null);
            this.error.set(false);
            this.subscription = this.scopesService.getHistory(scopeId, nodeId).subscribe({
                next: (entries) => this.entries.set(entries ?? []),
                error: () => {
                    this.entries.set([]);
                    this.error.set(true);
                },
            });
        });
    }

    protected label(property: string) {
        return ScopeMdsService.getLabel(this.widgets(), property);
    }

    protected values(property: string, values: string[] | undefined) {
        return (values ?? [])
            .map((v) => ScopeMdsService.getValueLabel(this.widgets(), property, v))
            .join(', ');
    }

    /** "bulk:<jobId>" / "skill:<id>" are shown as their kind */
    protected actor(actor: string) {
        const [kind] = actor?.split(':') ?? [];
        return ['bulk', 'skill'].includes(kind) ? kind.toUpperCase() : null;
    }
}
