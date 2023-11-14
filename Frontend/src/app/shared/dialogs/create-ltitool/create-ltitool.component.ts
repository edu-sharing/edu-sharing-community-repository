import {
    ApplicationRef,
    Component,
    EventEmitter,
    Input,
    NgZone,
    OnInit,
    Output,
} from '@angular/core';
import { LtiPlatformService, Tool } from 'ngx-edu-sharing-api';
import { DialogButton } from '../../../util/dialog-button';
import {
    Node,
    NodeWrapper,
    RestConstants,
    RestHelper,
    RestNodeService,
} from '../../../core-module/core.module';
import { NodeHelperService } from '../../../services/node-helper.service';

@Component({
    selector: 'es-create-ltitool',
    templateUrl: './create-ltitool.component.html',
    styleUrls: ['./create-ltitool.component.scss'],
})
export class CreateLtitoolComponent implements OnInit {
    public _tool: Tool;
    public _parent: Node;
    buttons: DialogButton[];
    @Output() onCancel = new EventEmitter();
    @Output() onCreate = new EventEmitter();
    public _name = '';
    nodes: Node[] = [];

    constructor(
        private ngZone: NgZone,
        private nodeService: RestNodeService,
        private nodeHelper: NodeHelperService,
        private ltiPlatformService: LtiPlatformService,
    ) {
        this.buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton('CREATE', { color: 'primary' }, () => this.create()),
        ];
        (window as any)['angularComponentReference'] = {
            component: this,
            zone: this.ngZone,
            loadAngularFunction: (nodeIds: string[], titles: string[]) =>
                this.deeplinkResponse(nodeIds, titles),
        };
    }

    ngOnInit(): void {
        if (!this._tool) {
            return;
        }
        console.log(
            'this._tool.appId24:' +
                this._tool.appId +
                ' parentId:' +
                this._parent.ref.id +
                ' nodes:' +
                this.nodes.length,
        );
        if (!this._tool.customContentOption) {
            this.openDeepLinkFlow();
        }
    }

    @Input() set tool(tool: Tool) {
        this._tool = tool;
    }

    @Input() set parent(parent: Node) {
        this._parent = parent;
    }

    public cancel() {
        this.onCancel.emit({ nodes: this.nodes });
    }

    public create() {
        console.log(
            'create() this._name:' +
                this._name +
                ' this._tool.customContentOption:' +
                this._tool.customContentOption,
        );
        if (this._tool.customContentOption) {
            this.createLtiContentOptionNode();
            return;
        } else {
            if (!this.nodes) {
                return;
            }
            this.onCreate.emit({ nodes: this.nodes, tool: this._tool });
        }
    }

    public createLtiContentOptionNode() {
        // @TODO cordova handling, popup problem
        console.log('open() this._name:' + this._name);
        if (this._name == undefined) {
            return;
        }
        const properties = RestHelper.createNameProperty(this._name);
        this.nodeService
            .createNode(this._parent.ref.id, RestConstants.CCM_TYPE_IO, [], properties)
            .subscribe(
                (data: NodeWrapper) => {
                    this.ltiPlatformService
                        .convertToLtiResourceLink(data.node.ref.id, this._tool.appId)
                        .subscribe(
                            (result: any) => {
                                this.nodes[0] = data.node;
                                /**
                                 * auto close when customContentOption and open resourcelink
                                 */
                                this.onCreate.emit({ nodes: this.nodes, tool: this._tool });
                            },
                            (error: any) => {
                                this.nodeHelper.handleNodeError(this._name, error);
                            },
                        );
                },
                (error: any) => {
                    this.nodeHelper.handleNodeError(this._name, error);
                },
            );
    }

    public openDeepLinkFlow() {
        let url =
            '/edu-sharing/rest/ltiplatform/v13/generateLoginInitiationForm?appId=' +
            this._tool.appId +
            '&parentId=' +
            this._parent.ref.id;
        let w = window.open(url, '_blank');

        if (!w) {
            window.alert('popups are disabled');
        }
    }

    public deeplinkResponse(nodeIds: string[], titles: string[]) {
        console.log('js function called ' + nodeIds + ' titles:' + titles + ' test');
        this._name = titles[0];

        let idx = 0;
        nodeIds.forEach((nodeId) => {
            let node = new Node();
            node.ref.id = nodeId;
            node.name = titles[idx];
            this.nodes[idx] = node;
            idx++;
        });
        console.log(this.nodes);
    }
}
