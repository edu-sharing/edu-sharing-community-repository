import {
    AfterViewInit,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    EventEmitter,
    Output,
    signal,
    ViewChild,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
import { DEFAULT, HOME_REPOSITORY, MdsWidget, SessionStorageService } from 'ngx-edu-sharing-api';
import { firstValueFrom, Subject } from 'rxjs';
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
    readonly selectedProfilingVariablesKey: string = 'selectedProfilingVariables';
    storageValues: { [p: string]: string[] } = {};
    initialValuesLoaded: WritableSignal<boolean> = signal(false);

    @Output() selectDimensionsChanged: EventEmitter<Map<string, MdsWidget>> = new EventEmitter<
        Map<string, MdsWidget>
    >();
    @ViewChild(MdsEditorWrapperComponent) mdsEditor: MdsEditorWrapperComponent;

    constructor(
        private genericWidgetGlobalService: GenericWidgetGlobalService,
        private storage: SessionStorageService,
        private toast: Toast,
        private topicPageHelperService: TopicPageHelperService,
    ) {
        this.mdsParams.setId = this.genericWidgetGlobalService.getDefaultMds();
    }

    async ngAfterViewInit(): Promise<void> {
        this.storageValues = (await this.storage.get(this.selectedProfilingVariablesKey)) || {};
        if (Object.keys(this.storageValues).length) {
            this.topicPageHelperService.setSelectedVariables(this.storageValues);
        }
        this.initialValuesLoaded.set(true);
        // wait for the view being refreshed
        setTimeout(() => {
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
        await this.storage.set(this.selectedProfilingVariablesKey, selectedValues);
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
