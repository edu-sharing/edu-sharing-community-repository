import {
    AfterViewInit,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    EventEmitter,
    Output,
    ViewChild,
    ViewEncapsulation,
} from '@angular/core';
import { DEFAULT, HOME_REPOSITORY, MdsWidget } from 'ngx-edu-sharing-api';
import { Values } from 'ngx-edu-sharing-ui';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MdsModule } from '../../../../features/mds/mds.module';
import { Widget } from '../../../../features/mds/mds-editor/mds-editor-instance.service';
import { MdsEditorWrapperComponent } from '../../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { Toast, ToastType } from '../../../../services/toast';
import { SharedModule } from '../../../../shared/shared.module';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { GenericWidgetGlobalService } from '../generic-widget/generic-widget-global.service';

@Component({
    selector: 'es-profiling',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [SharedModule, MdsModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './profiling.component.html',
    styleUrls: ['./profiling.component.scss'],
})
export class ProfilingComponent implements AfterViewInit {
    private destroy$ = new Subject<void>();
    mdsParams: { repository: string; setId: string } = {
        repository: HOME_REPOSITORY,
        setId: DEFAULT,
    };
    mdsExternalFilters: any = null;
    searchFilterValues: Values = {};

    @Output() selectDimensionsChanged: EventEmitter<Map<string, MdsWidget>> = new EventEmitter<
        Map<string, MdsWidget>
    >();
    @ViewChild(MdsEditorWrapperComponent) mdsEditor: MdsEditorWrapperComponent;

    constructor(
        private genericWidgetGlobalService: GenericWidgetGlobalService,
        private toast: Toast,
        private topicPageHelperService: TopicPageHelperService,
    ) {
        this.mdsParams.setId = this.genericWidgetGlobalService.getDefaultMds();
    }

    ngAfterViewInit(): void {
        this.mdsEditor.mdsEditorInstance.widgets
            .pipe(takeUntil(this.destroy$))
            .subscribe((widgets: Widget[]) => {
                if (!widgets?.length) {
                    return;
                }
                this.selectDimensionsChanged.emit(
                    new Map<string, MdsWidget>(
                        widgets.map((widget) => [widget.definition.id, widget.definition]),
                    ),
                );
            });
    }

    /**
     * Applies the latest selected query params and emit the select values.
     *
     * @param resetTrigger
     */
    async applySelectValues(resetTrigger: boolean = false): Promise<void> {
        let selectedValues = await this.mdsEditor.getValues();
        // filter out variables with empty value
        selectedValues = Object.fromEntries(
            Object.entries(selectedValues).filter(([, value]) => value && value.length > 0),
        );
        this.topicPageHelperService.setSelectedVariables(selectedValues);
        // inform the user about the changes
        this.toast.show({
            message: 'TOPIC_PAGE.WIDGET.PROFILING.CHANGES_' + (resetTrigger ? 'RESET' : 'SAVED'),
            type: 'info',
            subtype: ToastType.InfoSimple,
        });
    }

    /**
     * Resets all select values.
     */
    async resetSelectValues(): Promise<void> {
        await this.mdsEditor.mdsEditorInstance.clearValues();
        await this.applySelectValues(true);
    }
}
