import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';

/**
 * Reusable, selectable stat card.
 *
 * Designed to be reused across the usages view (and beyond): it renders a simple
 * icon/label/value header out of the box, but also projects arbitrary content
 * (e.g. an inline chart) via `<ng-content>`.
 *
 * The parent fully controls the selection:
 *  - `selectable` toggles whether the card reacts to clicks at all
 *    (a parent can therefore run in single-select, multi-select, or a purely
 *    read-only mode).
 *  - `selected` reflects the current selection state.
 *  - `disabled` fully disables the card (no interaction, dimmed).
 *
 * The component itself is stateless – it only emits `toggle` and lets the parent
 * decide how the selection set changes.
 */
@Component({
    selector: 'es-usage-stat-card',
    templateUrl: 'usage-stat-card.component.html',
    styleUrls: ['usage-stat-card.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [SharedModule],
})
export class UsageStatCardComponent {
    /** icon name used in the card header */
    readonly icon = input<string>();
    /** display label (already translated by the caller) */
    readonly label = input<string>();
    /** primary value/metric to display */
    readonly value = input<string | number | null>();
    /** optional accent color for the icon */
    readonly color = input<string>();
    /** optional badge label shown top-right (e.g. "coming soon") */
    readonly badge = input<string>();
    /** whether the user can toggle the card */
    readonly selectable = input(true);
    /** whether the card is currently selected */
    readonly selected = input(false);
    /** fully disables the card (no interaction, dimmed) */
    readonly disabled = input(false);

    /** emitted when the user activates a selectable, enabled card */
    readonly toggle = output<void>();

    protected readonly interactive = computed(() => this.selectable() && !this.disabled());

    protected onActivate(): void {
        if (this.interactive()) {
            this.toggle.emit();
        }
    }
}
