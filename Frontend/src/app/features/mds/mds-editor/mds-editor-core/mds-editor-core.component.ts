import { Component, QueryList, ViewChildren } from '@angular/core';
import { BehaviorSubject, firstValueFrom, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import { EditorMode, MdsView } from '../../types/types';
import { MdsEditorViewComponent } from '../mds-editor-view/mds-editor-view.component';
import { AuthenticationService, RestConstants } from 'ngx-edu-sharing-api';
import { Toast, ToastType } from '../../../../services/toast';
import { DialogsService } from '../../../dialogs/dialogs.service';
import { EduSharingLlmService, WidgetAiConfigInfo } from 'ngx-edu-sharing-b-api';
import { MdsEditorCommonService } from '../mds-editor-common.service';

@Component({
    selector: 'es-mds-editor-core',
    templateUrl: './mds-editor-core.component.html',
    styleUrls: ['./mds-editor-core.component.scss'],
})
export class MdsEditorCoreComponent {
    @ViewChildren('view') viewRef: QueryList<MdsEditorViewComponent>;

    views: MdsView[];
    suggestionsViews: MdsView[];
    hasExtendedWidgets$: Observable<boolean>;
    readonly editorMode: EditorMode;
    readonly shouldShowExtendedWidgets$: BehaviorSubject<boolean>;
    readonly hasAi = new BehaviorSubject(false);
    readonly aiLoading = new BehaviorSubject(false);

    constructor(
        public mdsEditorInstance: MdsEditorInstanceService,
        public mdsEditorCommonService: MdsEditorCommonService,
        public dialogs: DialogsService,
        public toast: Toast,
        public eduSharingLlmService: EduSharingLlmService,
        public auth: AuthenticationService,
    ) {
        this.shouldShowExtendedWidgets$ = this.mdsEditorInstance.shouldShowExtendedWidgets$;
        this.editorMode = this.mdsEditorInstance.editorMode;
        this.mdsEditorInstance.mdsInitDone.subscribe(() => this.init());
        this.hasExtendedWidgets$ = this.mdsEditorInstance.widgets.pipe(
            map((widgets) => widgets?.some((widget) => widget.definition.isExtended)),
        );
    }

    clear(): void {
        void this.mdsEditorInstance.clearValues();
    }

    private async init(): Promise<void> {
        if (this.views) {
            // Make sure existing views are destroyed and reinitialized.
            this.views = [];
            this.suggestionsViews = [];
            await tick();
        }
        this.mdsEditorInstance.resetWidgets();
        this.views = this.mdsEditorInstance.views.filter((view) => !view.rel);
        this.suggestionsViews = this.mdsEditorInstance.views.filter(
            (view) => view.rel === 'suggestions',
        );
        // Wait for `MdsEditorViewComponent`s to be injected.
        await tick();
        // Wait for `MdsEditorViewComponent`s to inject their widgets.
        await tick();
        this.mdsEditorInstance.mdsInflated.next(true);
        this.auth
            .hasToolpermission(RestConstants.TOOLPERMISSION_BAPI)
            .then((has) => this.hasAi.next(has && this.mdsEditorInstance.suggestionsSupported));
    }

    async generateSuggestions() {
        this.aiLoading.next(true);
        this.toast.show({
            message: 'MDS.AI.GENERATE_ASYNC_STARTED',
            type: 'info',
            subtype: ToastType.InfoAction,
        });
        try {
            const widgets: WidgetAiConfigInfo[] = this.mdsEditorInstance.widgets.value
                .filter(
                    (w) =>
                        !w.getIsDirty() &&
                        w.definition.aiConfigs?.length &&
                        this.mdsEditorInstance.nodes$.value?.length === 1,
                )
                .map((w) => {
                    return {
                        widgetId: w.definition.id,
                        aiConfigId: 'default', //w.definition.aiConfigs[0].id,
                    };
                });
            const values = await this.mdsEditorInstance.getValues(null, false);
            await firstValueFrom(
                this.eduSharingLlmService.suggestions({
                    body: {
                        user: (await firstValueFrom(this.auth.observeLoginInfo())).authorityName,
                        metadataSet: this.mdsEditorInstance.mdsId,
                        mdsAiConfigIds: ['suggestion_ai'], // [this.mdsEditorInstance.mdsDefinition$.value.aiConfigs.find(a => a.id === 'suggestion_ai').id],
                        widgetAiConfigs: widgets,
                        contextNodeId: this.mdsEditorInstance.nodes$.value[0].ref.id,
                        variables: values,
                    },
                }),
            );
            this.mdsEditorInstance.suggestionMetadata$.next(
                await this.mdsEditorCommonService.fetchNodesSuggestions(
                    this.mdsEditorInstance.nodes$.value,
                ),
            );
            /*this.mdsEditorInstance.suggestionMetadata$.next([{
                suggestions: {
                    'cclom:title': [{
                        value: 'Hello World',
                        type: 'AI',
                        status: 'PENDING',
                    }]
                }
            }])*/
        } catch (e) {
            console.warn('Could not fetch suggestion data', e);
        }
        this.toast.show({
            message: 'MDS.AI.GENERATE_ASYNC_FINISHED',
            type: 'info',
            subtype: ToastType.InfoAction,
        });
        this.aiLoading.next(false);
    }
}

function tick(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve));
}
