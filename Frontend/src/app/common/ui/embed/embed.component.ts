import { Component, NgZone, ViewChild, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {
    EventListener,
    FrameEventsService,
} from '../../../core-module/rest/services/frame-events.service';
import { TranslationsService } from '../../../translations/translations.service';
import { WorkspaceLicenseComponent } from '../../../modules/management-dialogs/license/license.component';
import { Toast } from '../../../core-ui-module/toast';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { MainNavService } from '../../../main/navigation/main-nav.service';
import { MdsEditorWrapperComponent } from '../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';

@Component({
    selector: 'es-mds-embed',
    encapsulation: ViewEncapsulation.None,
    template: `
        <es-mds-editor-wrapper
            #mdsRef
            [embedded]="true"
            editorMode="form"
            [currentValues]="data"
            [setId]="setId"
            [groupId]="groupId"
            *ngIf="component === 'mds'"
        ></es-mds-editor-wrapper>
        <es-workspace-license
            #licenseRef
            [properties]="data"
            [embedded]="true"
            *ngIf="component === 'license'"
        ></es-workspace-license>
    `,
    styleUrls: ['embed.component.scss'],
})
export class EmbedComponent implements EventListener {
    @ViewChild('mdsRef') mdsRef: MdsEditorWrapperComponent;
    @ViewChild('licenseRef') licenseRef: WorkspaceLicenseComponent;
    component: string;
    data: any = {};
    groupId = 'io';
    setId = RestConstants.DEFAULT;
    refresh: Boolean;
    constructor(
        private translations: TranslationsService,
        private mainNavService: MainNavService,
        private toast: Toast,
        private ngZone: NgZone,
        private route: ActivatedRoute,
        private event: FrameEventsService,
    ) {
        (window as any).ngEmbed = this;
        // disable the cookie info when in embedded context
        this.mainNavService.getCookieInfo().show = false;
        this.mainNavService.patchMainNavConfig({
            currentScope: 'embed',
        });
        this.event.addListener(this);
        this.toast.showProgressDialog();
        this.translations.waitForInit().subscribe(() => {
            this.route.params.subscribe((params) => {
                this.component = params.component;
                this.route.queryParams.subscribe((params) => {
                    if (params.group) {
                        this.groupId = params.group;
                    }
                    if (params.set) {
                        this.setId = params.set;
                    }
                    if (params.data) {
                        this.data = JSON.parse(params.data);
                    }
                    if (this.component === 'mds') {
                        UIHelper.waitForComponent(this.ngZone, this, 'mdsRef').subscribe(
                            async () => {
                                await this.mdsRef.reInit();
                                this.toast.closeModalDialog();
                            },
                        );
                    } else {
                        this.toast.closeModalDialog();
                    }
                });
            });
        });
    }
    async onEvent(event: string, data: any) {
        if (event === FrameEventsService.EVENT_PARENT_FETCH_DATA) {
            if (this.component === 'mds') {
                this.event.broadcastEvent(
                    FrameEventsService.EVENT_POST_DATA,
                    await this.mdsRef.getValues(),
                );
            } else if (this.component === 'license') {
                this.event.broadcastEvent(
                    FrameEventsService.EVENT_POST_DATA,
                    this.licenseRef.getProperties(),
                );
            }
        }
    }
}
