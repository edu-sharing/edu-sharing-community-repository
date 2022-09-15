import {
    ApplicationRef,
    Component,
    EventEmitter,
    Input,
    NgZone,
    OnInit,
    Output,
} from '@angular/core';
import { Tool } from 'ngx-edu-sharing-api';
import { DialogButton } from '../../../core-module/ui/dialog-button';
import {
    Node,
    NodeWrapper,
    RestConstants,
    RestHelper,
    RestNodeService,
} from '../../../core-module/core.module';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';

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
    public _nodeIds: string[];
    public _titles: string[];
    nodes: Node[] = [];

    constructor(
        private ngZone: NgZone,
        private nodeService: RestNodeService,
        private nodeHelper: NodeHelperService,
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
        console.log(
            'this._tool.appId24:' +
                this._tool.appId +
                ' parentId:' +
                this._parent.ref.id +
                ' nodes:' +
                this.nodes.length,
        );
        if (!this._tool.customContentOption) {
            this.openLti();
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
            this.openLti();
            return;
        } else {
            if (!this.nodes) {
                return;
            }
            this.onCreate.emit({ nodes: this.nodes, tool: this._tool });
        }
    }

    public openLti() {
        // @TODO cordova handling, popup problem
        console.log('open() this._tool.customContentOption:' + this._tool.customContentOption);
        let url =
            '/edu-sharing/rest/ltiplatform/v13/generateLoginInitiationForm?appId=' +
            this._tool.appId +
            '&parentId=' +
            this._parent.ref.id;
        if (this._tool.customContentOption) {
            console.log('open() this._name:' + this._name);
            if (this._name == undefined) {
                return;
            }
            const properties = RestHelper.createNameProperty(this._name);
            this.nodeService
                .createNode(this._parent.ref.id, RestConstants.CCM_TYPE_IO, [], properties)
                .subscribe(
                    (data: NodeWrapper) => {
                        url = url + '&nodeId=' + data.node.ref.id;
                        window.open(url, '_blank');
                    },
                    (error: any) => {
                        this.nodeHelper.handleNodeError(this._name, error);
                    },
                );
        } else {
            let w = window.open(url, '_blank');

            if (!w) {
                window.alert('popups are disabled');
            }
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

        /**
         * auto close when customContentOption
         */
        if (this._tool.customContentOption) {
            this.onCreate.emit({ nodes: this.nodes, tool: this._tool });
        }
    }
}
