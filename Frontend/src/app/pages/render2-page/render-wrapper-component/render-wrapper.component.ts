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
    standalone: true,
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
        /*this.dummyRequest.set({
            nodeId: 'TEST_lviv.jpg',
            size: -1,
            type: 'file-image',
            hash: '1',
            mimeType: 'image/jpeg',
            version: '1.0',
            repoId: '',
        });

        this.request.set({
            nodeId: 'TEST_4k.mp4',
            size: -1,
            type: 'file-video',
            hash: '' + Math.random(),
            mimeType: 'video/mpeg',
            version: '1.0',
            repoId: '',
        });

        this.request.set({
            nodeId: 'TEST_portrait.pdf',
            size: -1,
            type: 'file-pdf',
            hash: '' + Math.random(),
            mimeType: 'application/pdf',
            version: '1.0',
            repoId: '',
        });

        /*this.dummyRequest.set({
            nodeId: 'TEST_lorem_ipsum.odt',
            size: -1,
            type: 'file-word',
            hash: '' + Math.random(),
            mimeType: 'application/vnd.oasis.opendocument.text',
            version: '1.0',
            repoId: '',
        });*/
        // url module
        this.data.set(undefined);
        /*
        combineLatest([this.route.params, this.route.queryParams]).subscribe(
            ([params, queryParams]) => {
                this.nodeApi
                    .getNode(params.node, {
                        repository: queryParams.repo || HOME_REPOSITORY,
                    })
                    .subscribe((n) => {
                        console.log(n);
                        this.node.set(n);
                        this.dummyRequest.set({
                            nodeId: 'TEST_lviv.jpg',
                            size: -1,
                            type: 'file-image',
                            hash: '1',
                            mimeType: 'image/jpeg',
                            version: '1.0',
                            repoId: '',
                        });

                    });
            },
        );
        */
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.nodeId) {
            const data = await this.renderHelperService.getRenderData(
                changes.nodeId.currentValue,
                this.version,
            );
            setTimeout(async () => {
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
