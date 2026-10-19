import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { merge, of } from 'rxjs';
import { map, startWith, switchMap } from 'rxjs/operators';
import { NodeEntriesService } from '../../services/node-entries.service';
import { NodeEntriesDataType } from '../data-type';

/**
 * Offers to select all results (not only the loaded ones) once all loaded entries are selected,
 * and shows the state afterwards. Visible only if the host enabled `allowSelectAllResults`.
 */
@Component({
    selector: 'es-node-entries-select-all-banner',
    templateUrl: './node-entries-select-all-banner.component.html',
    styleUrls: ['./node-entries-select-all-banner.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class NodeEntriesSelectAllBannerComponent<T extends NodeEntriesDataType> {
    protected readonly entriesService = inject<NodeEntriesService<T>>(NodeEntriesService);

    /** re-evaluates whenever selection, data or the chosen state changes */
    private readonly changes = toSignal(
        merge(
            this.entriesService.selection.changed,
            this.entriesService.dataSource$.pipe(
                switchMap((dataSource) => dataSource?.connect() ?? of(null)),
            ),
            toObservable(this.entriesService.allResultsSelected),
            toObservable(this.entriesService.allowSelectAllResults),
        ).pipe(
            map(() => ({})),
            startWith({}),
        ),
    );

    protected readonly state = computed<'offer' | 'active' | null>(() => {
        this.changes();
        if (!this.entriesService.allowSelectAllResults()) {
            return null;
        }
        if (this.entriesService.allResultsSelected()) {
            return 'active';
        }
        return this.entriesService.canSelectAllResults() ? 'offer' : null;
    });

    protected total(): number {
        return this.entriesService.dataSource?.getTotal() ?? 0;
    }

    protected loaded(): number {
        return this.entriesService.dataSource?.getData().length ?? 0;
    }
}
