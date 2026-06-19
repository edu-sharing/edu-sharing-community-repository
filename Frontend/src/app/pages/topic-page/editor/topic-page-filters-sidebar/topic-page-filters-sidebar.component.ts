import { Component, EventEmitter, Output, inject } from '@angular/core';
import { HOME_REPOSITORY } from 'ngx-edu-sharing-api';
import { MdsModule } from '../../../../features/mds/mds.module';
import { Values } from '../../../../features/mds/types/types';
import { SharedModule } from '../../../../shared/shared.module';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';

@Component({
    selector: 'es-topic-page-filters-sidebar',
    imports: [SharedModule, MdsModule],
    templateUrl: './topic-page-filters-sidebar.component.html',
    styleUrls: ['./topic-page-filters-sidebar.component.scss'],
})
export class TopicPageFiltersSidebarComponent {
    genericWidgetGlobalService = inject(GenericWidgetGlobalService);

    @Output() closeFilterbar: EventEmitter<void> = new EventEmitter<void>();
    @Output() currentValuesChange: EventEmitter<Values> = new EventEmitter<Values>();

    /**
     * Reacts to the currentValuesChange event and emits it the same way.
     *
     * @param selectedValues
     */
    applySearchFilters(selectedValues: Values): void {
        selectedValues = Object.fromEntries(
            Object.entries(selectedValues).filter(([, value]) => value && value.length > 0),
        );
        this.currentValuesChange.emit(selectedValues);
    }

    /**
     * Emits a close event for the filter bar.
     */
    closeFilterBar(): void {
        this.closeFilterbar.emit();
    }

    protected readonly HOME_REPOSITORY = HOME_REPOSITORY;
}
