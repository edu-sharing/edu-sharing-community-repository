import { Component } from '@angular/core';
import { TranslationsService } from '../../../../projects/edu-sharing-ui/src/lib/translations/translations.service';
import {
    FrameEventsService,
    Node,
    RequestObject,
    RestCollectionService,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestIamService,
    RestMdsService,
    RestNodeService,
    RestToolService,
    TemporaryStorageService,
    UIService,
} from '../../core-module/core.module';
import { Router } from '@angular/router';
import { Toast } from '../../core-ui-module/toast';
import { trigger } from '@angular/animations';
import { CordovaService } from '../../common/services/cordova.service';
import { HttpClient } from '@angular/common/http';
import { BridgeService } from '../../core-bridge-module/bridge.service';
import { CardService } from '../../core-ui-module/card.service';
import { ListItem, NodeDataSource, Scope, UIAnimation } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-editorial',
    templateUrl: 'editorial.component.html',
    styleUrls: ['editorial.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('fadeFast', UIAnimation.fade(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('fromLeft', UIAnimation.fromLeft()),
        trigger('fromRight', UIAnimation.fromRight()),
    ],
})
export class EditorialComponent {
    public readonly SCOPES = Scope;
    nodeDataSource = new NodeDataSource<Node>();
    columns: ListItem[];

    constructor(
        private toast: Toast,
        private bridge: BridgeService,
        private router: Router,
        private http: HttpClient,
        private translations: TranslationsService,
        private storage: TemporaryStorageService,
        private connectors: RestConnectorsService,
        private collectionApi: RestCollectionService,
        private toolService: RestToolService,
        private iam: RestIamService,
        private mds: RestMdsService,
        private nodeService: RestNodeService,
        private ui: UIService,
        private event: FrameEventsService,
        private connector: RestConnectorService,
        private cordova: CordovaService,
        private card: CardService,
    ) {
        this.columns = [
            new ListItem('NODE', RestConstants.LOM_PROP_TITLE),
            new ListItem('NODE', RestConstants.CCM_PROP_WF_STATUS),
            new ListItem('NODE', RestConstants.CCM_PROP_WF_INSTRUCTIONS),
            new ListItem('NODE', RestConstants.CM_MODIFIED_DATE),
            new ListItem('NODE', RestConstants.CM_CREATOR),
        ];
        this.translations.waitForInit().subscribe(() => {
            this.initialize();
        });
    }

    private async loadNodes() {
        const request: RequestObject = {
            offset: this.nodeDataSource.getData()?.length,
            propertyFilter: [RestConstants.ALL],
        };
        this.nodeDataSource.isLoading = true;
        const nodes = await this.nodeService
            .getChildren(RestConstants.WORKFLOW_RECEIVE, null, request)
            .toPromise();
        this.nodeDataSource.isLoading = false;
        this.nodeDataSource.appendData(nodes.nodes);
        this.nodeDataSource.setPagination(nodes.pagination);
    }

    private initialize() {
        this.nodeDataSource.reset();
        this.loadNodes();
    }
}
