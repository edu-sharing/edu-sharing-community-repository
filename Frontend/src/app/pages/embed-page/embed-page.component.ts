import { Component, NgZone, OnDestroy, ViewChild, ViewEncapsulation, inject } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import {
    EventListener,
    FrameEventsService,
} from '../../core-module/rest/services/frame-events.service';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { LicenseDialogContentComponent } from '../../features/dialogs/dialog-modules/license-dialog/license-dialog-content.component';
import { Toast } from '../../services/toast';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { MdsEditorWrapperComponent } from '../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { Subject } from 'rxjs';
import { WIDGETS } from '../topic-page/shared/types/custom-definitions';

@Component({
    selector: 'es-mds-embed',
    encapsulation: ViewEncapsulation.None,
    template: `
        @if (!component) { Please append /&lt;component-name&gt; to your url } @if (component ===
        'mds') {
        <es-mds-editor-wrapper
            #mdsRef
            [embedded]="true"
            editorMode="form"
            [currentValues]="data"
            [setId]="setId"
            [groupId]="groupId"
        ></es-mds-editor-wrapper>
        } @if (component === 'license') {
        <es-license-dialog-content
            #licenseRef
            [data]="{ kind: 'properties', properties: data }"
        ></es-license-dialog-content>
        } @if (component === 'generic-widget') {
        <es-generic-widget
            [contextNodeId]="queryParams.contextNodeId"
            [widgetType]="queryParams.widgetType || WIDGETS.CONTENT_TEASER"
            [configOverwrite]="configOverwrite"
            [editMode]="queryParams.editMode === 'true'"
        ></es-generic-widget>
        }
    `,
    styleUrls: ['embed-page.component.scss'],
    standalone: false,
})
export class EmbedPageComponent implements EventListener, OnDestroy {
    private translations = inject(TranslationsService);
    private mainNavService = inject(MainNavService);
    private toast = inject(Toast);
    private ngZone = inject(NgZone);
    private route = inject(ActivatedRoute);
    private event = inject(FrameEventsService);

    @ViewChild('mdsRef') mdsRef: MdsEditorWrapperComponent;
    @ViewChild('licenseRef') licenseRef: LicenseDialogContentComponent;
    readonly jsonConfig: any = {
        minimap: { enabled: false },
        language: 'json',
        automaticLayout: true,
    };
    component: 'mds' | 'license' | 'generic-widget';
    configOverwrite: string;
    data: any = {};
    groupId = 'io';
    setId = RestConstants.DEFAULT;
    refresh: Boolean;
    private destroyed = new Subject<void>();
    queryParams: Params;
    constructor() {
        (window as any).ngEmbed = this;
        // disable the cookie info when in embedded context
        this.mainNavService.getCookieInfo().show = false;
        this.mainNavService.patchMainNavConfig({
            currentScope: 'embed',
            show: false,
        });
        this.event.addListener(this, this.destroyed);
        this.toast.showProgressSpinner();
        this.translations.waitForInit().subscribe(() => {
            this.route.params.subscribe((params) => {
                this.component = params.component;
                this.route.queryParams.subscribe((params) => {
                    this.queryParams = params;
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
                                this.toast.closeProgressSpinner();
                            },
                        );
                    } else {
                        this.toast.closeProgressSpinner();
                    }
                });
            });
        });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
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

    protected readonly WIDGETS = WIDGETS;
}
