import { Component, Input, OnChanges, signal, SimpleChanges, ViewChild } from '@angular/core';
import {
    ActionbarComponent,
    CombinedRenderData,
    EduSharingUiModule,
    OptionsHelperDataService,
    RenderHelperService,
    TranslationsService,
} from 'ngx-edu-sharing-ui';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { RenderComponent, RenderingServiceLibModule } from 'ngx-rendering-service-lib';
import { MdsModule } from '../../../features/mds/mds.module';
import { SharedModule } from '../../../shared/shared.module';

@Component({
    selector: 'es-render-wrapper-component',
    templateUrl: 'render-wrapper.component.html',
    styleUrls: ['render-wrapper.component.scss'],
    imports: [
        CommonModule,
        EduSharingUiModule,
        MatButtonModule,
        RenderComponent,
        SharedModule,
        RenderingServiceLibModule,
        MdsModule,
    ],
    // required for optional mds module
    providers: [OptionsHelperDataService, RenderHelperService],
})
export class RenderWrapperComponent implements OnChanges {
    @ViewChild(ActionbarComponent) actionbar: ActionbarComponent;
    @Input() nodeId: string;
    @Input() version: string;

    data = signal<CombinedRenderData>(null);

    constructor(
        private renderHelperService: RenderHelperService,
        private translations: TranslationsService,
        private optionsHelper: OptionsHelperDataService,
    ) {
        this.translations.waitForInit().subscribe(() => {});
        this.optionsHelper.registerGlobalKeyboardShortcuts();
        this.data.set(undefined);
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.nodeId) {
            const data = await this.renderHelperService.getRenderData(
                changes.nodeId.currentValue,
                this.version,
            );
            setTimeout(async () => {
                console.log('actions');
                await this.optionsHelper.initComponents(this.actionbar);
                await this.optionsHelper.refreshComponents();
            });
            this.data.set(data);
        }
    }

    goBack() {
        window.history.back();
    }
}
