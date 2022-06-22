import { Component, ElementRef, HostListener, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { TranslationsService } from '../../translations/translations.service';
import {
    ClipboardObject,
    Connector,
    ConnectorList,
    DialogButton,
    EventListener,
    Filetype,
    FrameEventsService,
    IamUser,
    ListItem,
    LoginResult,
    Node,
    NodeList,
    NodeRef,
    NodeVersions,
    NodeWrapper,
    RequestObject,
    RestCollectionService,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestMdsService,
    RestNodeService,
    RestToolService,
    TemporaryStorageService,
    UIService,
    Version,
} from '../../core-module/core.module';
import { Params, Router } from '@angular/router';
import { OptionItem, Scope } from '../../core-ui-module/option-item';
import { Toast } from '../../core-ui-module/toast';
import { UIAnimation } from '../../core-module/ui/ui-animation';
import { trigger } from '@angular/animations';
import { ActionbarHelperService } from '../../common/services/actionbar-helper';
import { CordovaService } from '../../common/services/cordova.service';
import { HttpClient } from '@angular/common/http';
import { BridgeService } from '../../core-bridge-module/bridge.service';
import { CardService } from '../../core-ui-module/card.service';
import { Observable } from 'rxjs';
import { ListTableComponent } from '../../core-ui-module/components/list-table/list-table.component';

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
    @ViewChild('list') list: ListTableComponent;
    public readonly SCOPES = Scope;
    nodes: Node[];
    columns: ListItem[];

    constructor(
        private toast: Toast,
        private bridge: BridgeService,
        private router: Router,
        private http: HttpClient,
        private translations: TranslationsService,
        private storage: TemporaryStorageService,
        private connectors: RestConnectorsService,
        private actionbar: ActionbarHelperService,
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
            offset: this.nodes?.length,
            propertyFilter: [RestConstants.ALL],
        };
        this.list.isLoading = true;
        const nodes = await this.nodeService
            .getChildren(RestConstants.WORKFLOW_RECEIVE, null, request)
            .toPromise();
        this.list.isLoading = false;
        this.nodes = this.nodes.concat(nodes.nodes);
        this.list.totalCount = nodes.pagination.total;
        this.list.hasMore = this.nodes.length < nodes.pagination.total;
    }

    private initialize() {
        this.nodes = [];
        this.loadNodes();
    }
}
