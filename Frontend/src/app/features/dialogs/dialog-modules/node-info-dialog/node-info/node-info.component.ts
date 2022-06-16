import { Component, Inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    ConfigurationHelper,
    ConfigurationService,
    Node,
    NodeList,
    Permissions,
    RestConstants,
    RestNodeService,
} from '../../../../../core-module/core.module';
import { UIConstants } from '../../../../../core-module/ui/ui-constants';
import { Toast } from '../../../../../core-ui-module/toast';
import { CARD_DIALOG_DATA, configForNode } from '../../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../../card-dialog/card-dialog-ref';
import {UIHelper} from "../../../../../core-ui-module/ui-helper";

export interface NodeInfoDialogData {
    node: Node;
}

@Component({
    selector: 'es-node-info',
    templateUrl: 'node-info.component.html',
    styleUrls: ['node-info.component.scss'],
})
/**
 * A node info dialog (useful primary for admin stuff)
 */
export class NodeInfoComponent implements OnInit {
    _node: Node;
    _path: Node[];
    _children: Node[];
    _permissions: Permissions;
    _properties: any[];
    _creator: string;
    _json: string;
    saving: boolean;
    customProperty: string[] = [];
    editMode: boolean;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: NodeInfoDialogData,
        private dialogRef: CardDialogRef,
        private nodeApi: RestNodeService,
        private toast: Toast,
        private config: ConfigurationService,
        private router: Router,
        private translate: TranslateService,
    ) {}

    ngOnInit(): void {
        this.setNode(this.data.node);
    }

    private setNode(node: Node): void {
        this._node = node;
        this.translate
            .get('NODE_INFO.TITLE', { name: node.name })
            .subscribe((title) => this.dialogRef.patchConfig({ title, ...configForNode(node) }));
        this._creator = ConfigurationHelper.getPersonWithConfigDisplayName(
            node.createdBy,
            this.config,
        );
        this._json = JSON.stringify(node, null, 4);
        this._properties = [];
        if (node.properties) {
            for (let k of Object.keys(node.properties).sort()) {
                if (node.properties[k].join(''))
                    this._properties.push([k, node.properties[k].join(', ')]);
            }
        }
        this.nodeApi.getNodeParents(node.ref.id, true).subscribe((data: NodeList) => {
            this._path = data.nodes.reverse();
        });
        this.nodeApi
            .getChildren(node.ref.id, [RestConstants.FILTER_SPECIAL], {
                propertyFilter: [RestConstants.ALL],
                count: RestConstants.COUNT_UNLIMITED,
            })
            .subscribe((data: NodeList) => {
                this._children = data.nodes;
            });
        this.nodeApi.getNodePermissions(node.ref.id).subscribe((data) => {
            this._permissions = data.permissions;
        });
    }

    openNode(node: Node) {
        this._path = null;
        this._children = null;
        this.setNode(node);
    }
    openNodeWorkspace(node: Node) {
        this.router.navigate([UIConstants.ROUTER_PREFIX, 'workspace'], {
            queryParams: { id: node.parent.id, file: node.ref.id },
        });
        this.dialogRef.close();
    }
    openBreadcrumb(pos: number) {
        let node = this._path[pos - 1];
        this._path = null;
        this._children = null;
        this.setNode(node);
        //this.router.navigate([UIConstants.ROUTER_PREFIX,"workspace"],{queryParams:{id:node.ref.id}});
        //this.close();
    }

    canEdit() {
        return this._node && this._node.access.indexOf(RestConstants.ACCESS_WRITE) != -1;
    }

    addProperty() {
        if (this.customProperty[0]) {
            this.saving = true;
            this.nodeApi
                .editNodeProperty(
                    this._node.ref.id,
                    this.customProperty[0],
                    this.customProperty[1].split(','),
                    this._node.ref.repo,
                )
                .subscribe(
                    () => {
                        this.customProperty = [];
                        this.refreshMeta();
                    },
                    (error) => {
                        this.toast.error(error);
                        this.saving = false;
                    },
                );
        }
    }

    saveProperty(property: string[]) {
        this.nodeApi
            .editNodeProperty(
                this._node.ref.id,
                property[0],
                property[1].split(','),
                this._node.ref.repo,
            )
            .subscribe(
                () => {
                    this.customProperty = [];
                    this.refreshMeta();
                },
                (error) => {
                    this.toast.error(error);
                    this.saving = false;
                },
            );
    }

    private refreshMeta() {
        this.nodeApi
            .getNodeMetadata(this._node.ref.id, [RestConstants.ALL], this._node.ref.repo)
            .subscribe((node) => {
                this.saving = false;
                this.openNode(node.node);
            });
    }

    copyNodeIdToClipboard() {
        UIHelper.copyToClipboard(this._node.ref.id);
        this.toast.toast('ADMIN.APPLICATIONS.COPIED_CLIPBOARD');
    }
}
