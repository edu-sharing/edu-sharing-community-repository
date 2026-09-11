import {
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    Signal,
    WritableSignal,
    inject,
    signal,
    viewChild,
} from '@angular/core';
import { HOME_REPOSITORY } from 'ngx-edu-sharing-api';
import { MdsEditorWrapperComponent } from '../../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { MdsModule } from '../../../../features/mds/mds.module';
import { Values } from '../../../../features/mds/types/types';
import { SharedModule } from '../../../../shared/shared.module';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';

/**
 * Whether a set of MDS values holds a value at all.
 *
 * @param values
 */
function containsValues(values: Values): boolean {
    return Object.keys(values ?? {}).length > 0;
}

@Component({
    selector: 'es-topic-page-filters-sidebar',
    imports: [SharedModule, MdsModule],
    templateUrl: './topic-page-filters-sidebar.component.html',
    styleUrls: ['./topic-page-filters-sidebar.component.scss'],
})
export class TopicPageFiltersSidebarComponent implements OnInit {
    genericWidgetGlobalService = inject(GenericWidgetGlobalService);

    private readonly mdsEditor: Signal<MdsEditorWrapperComponent | undefined> =
        viewChild(MdsEditorWrapperComponent);
    // whether any filter is set, which is what the reset button is offered for
    protected readonly hasFilters: WritableSignal<boolean> = signal(false);

    // the values the MDS editor starts with
    @Input() currentValues: Values = {};

    @Output() closeFilterbar: EventEmitter<void> = new EventEmitter<void>();
    @Output() currentValuesChange: EventEmitter<Values> = new EventEmitter<Values>();

    ngOnInit(): void {
        this.hasFilters.set(containsValues(this.currentValues));
    }

    /**
     * Reacts to the currentValuesChange event and emits it the same way.
     *
     * @param selectedValues
     */
    applySearchFilters(selectedValues: Values): void {
        selectedValues = Object.fromEntries(
            Object.entries(selectedValues).filter(([, value]) => value && value.length > 0),
        );
        this.hasFilters.set(containsValues(selectedValues));
        this.currentValuesChange.emit(selectedValues);
    }

    /**
     * Drops every filter, both in the MDS editor and for the listeners of this sidebar.
     */
    protected async resetFilters(): Promise<void> {
        // the editor reads its values on initialization only
        this.currentValues = {};
        await this.mdsEditor()?.reInit();
        this.applySearchFilters({});
    }

    /**
     * Emits a close event for the filter bar.
     */
    closeFilterBar(): void {
        this.closeFilterbar.emit();
    }

    protected readonly HOME_REPOSITORY = HOME_REPOSITORY;
}
