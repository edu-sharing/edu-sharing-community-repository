import {Component, ViewChild, ViewEncapsulation} from "@angular/core";
import {ActivatedRoute} from "@angular/router";
import {EventListener, FrameEventsService} from "../../../core-module/rest/services/frame-events.service";
import {MdsComponent} from "../mds/mds.component";
import {TranslateService} from "@ngx-translate/core";
import {ConfigurationService} from "../../../core-module/rest/services/configuration.service";
import {SessionStorageService} from "../../../core-module/rest/services/session-storage.service";
import {Translation} from "../../../core-ui-module/translation";
import {WorkspaceLicenseComponent} from "../../../modules/management-dialogs/license/license.component";

@Component({
    selector: 'mds-embed',
    encapsulation: ViewEncapsulation.None,
    template: `
        <mds #mdsRef [embedded]="true" [currentValues]="data" [invalidate]="refresh" [groupId]="groupId" *ngIf="component=='mds'"></mds>
        <workspace-license #licenseRef [properties]="data" [embedded]="true" *ngIf="component=='license'"></workspace-license>
    `,
    styleUrls: ['embed.component.scss']
})
export class EmbedComponent implements EventListener{
    @ViewChild('mdsRef') mdsRef : MdsComponent;
    @ViewChild('licenseRef') licenseRef : WorkspaceLicenseComponent;
    component:string;
    data:any={};
    groupId = 'io';
    refresh:Boolean;
    constructor(private translate:TranslateService,
                private config:ConfigurationService,
                private storage:SessionStorageService,
                private route:ActivatedRoute,
                private event : FrameEventsService){
        this.event.addListener(this);
        Translation.initialize(this.translate,this.config,this.storage,this.route).subscribe(()=> {
            this.route.params.subscribe((params)=>{
               this.component=params['component'];
                this.route.queryParams.subscribe((params) => {
                    if (params['group'])
                        this.groupId = params['group'];
                    if (params['data'])
                        this.data = JSON.parse(params['data']);
                    this.refresh = new Boolean(true);
                });
            });
        });
    }

    onEvent(event: string, data: any): void {
        if(event==FrameEventsService.EVENT_PARENT_FETCH_DATA){
            if(this.component=='mds')
                this.event.broadcastEvent(FrameEventsService.EVENT_POST_DATA,this.mdsRef.getValues());
            if(this.component=='license')
                this.event.broadcastEvent(FrameEventsService.EVENT_POST_DATA,this.licenseRef.getProperties());
        }
    }
}
