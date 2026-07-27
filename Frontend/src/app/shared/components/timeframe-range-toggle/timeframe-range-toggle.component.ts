import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { SharedModule } from '../../shared.module';

/** Sentinel range value meaning "all time" (no lower bound); rendered as "all". */
export const ALL_RANGE = -1;

/** Predefined date range presets, expressed in days ({@link ALL_RANGE} = all time). */
export const DEFAULT_RANGE_OPTIONS = [7, 30, 90, ALL_RANGE] as const;

/**
 * A `mat-button-toggle-group` offering a set of predefined day-range presets
 * (e.g. "last 7 days", "last 30 days", … and a final "all" entry). Emits the
 * selected range in days. Shared by the usages preview and the admin statistics
 * "by object" panel.
 */
@Component({
    selector: 'es-timeframe-range-toggle',
    templateUrl: 'timeframe-range-toggle.component.html',
    styleUrls: ['timeframe-range-toggle.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [SharedModule],
})
export class TimeframeRangeToggleComponent {
    /** Predefined day-range presets; a negative value ({@link ALL_RANGE}) is labelled "all". */
    readonly rangeOptions = input<readonly number[]>(DEFAULT_RANGE_OPTIONS);
    /** Currently selected range in days (null/undefined = no preset selected). */
    readonly selectedRange = input<number | null>();
    /** Emits the newly selected range in days. */
    readonly rangeChange = output<number>();
}
