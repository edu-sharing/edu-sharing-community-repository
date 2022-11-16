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
import { forkJoin } from 'rxjs';
import { UIConstants } from '../../../../../core-module/ui/ui-constants';
import { Toast } from '../../../../../core-ui-module/toast';
import {
    CARD_DIALOG_DATA,
    configForNode,
    configForNodes
} from '../../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../../card-dialog/card-dialog-ref';
import {UIHelper} from "../../../../../core-ui-module/ui-helper";

export interface NodeInfoDialogData {
    nodes: Node[];
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
    _nodes: Node[];
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
        this.setNodes(this.data.nodes);
    }

    private setNodes(nodes: Node[]): void {
        this._nodes = nodes;
        this.translate
            .get('NODE_INFO.TITLE', { name: this._nodes[0].name })
            .subscribe((title) => this.dialogRef.patchConfig({ title }));
        configForNodes(nodes, this.translate).subscribe(
            config => this.dialogRef.patchConfig(config)
        );
        this._properties = [];
        nodes.filter(n => n.properties).forEach(n => {
            for (let k of Object.keys(n.properties).sort()) {
                if (n.properties[k].join('')) {
                    const value = n.properties[k].join(', ');
                    const current = this._properties.filter((n => n[0] === k));
                    if(!current.length) {
                        this._properties.push([k,value]);
                    }  else if(current[0][1] === value) {
                        // do nothing
                    }  else {
                        this._properties.splice(this._properties.indexOf(current[0]), 1);
                        this._properties.push([k,'[VARYING VALUES]']);
                    }
                }
            }
        })
        if(this._nodes.length === 1) {
            const node = nodes[0];
            this._creator = ConfigurationHelper.getPersonWithConfigDisplayName(
                node.createdBy,
                this.config,
            );
            this._json = JSON.stringify(node, null, 4);
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
    }

    openNodes(nodes: Node[]) {
        this._path = null;
        this._children = null;
        this.setNodes(nodes);
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
        this.setNodes([node]);
        //this.router.navigate([UIConstants.ROUTER_PREFIX,"workspace"],{queryParams:{id:node.ref.id}});
        //this.close();
    }

    canEdit() {
        return this._nodes?.every(n => n.access?.indexOf(RestConstants.ACCESS_WRITE) != -1);
    }

    addProperty() {
        if (this.customProperty[0]) {
            this.saving = true;
            forkJoin(this._nodes.map(n =>
                this.nodeApi
                    .editNodeProperty(
                        n.ref.id,
                        this.customProperty[0],
                        this.customProperty[1].split(','),
                        n.ref.repo,
                    )
            )).subscribe(
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
        this.saving = true;
        forkJoin(this._nodes.map(n =>
            this.nodeApi
                .editNodeProperty(
                    n.ref.id,
                    property[0],
                    property[1].split(','),
                    n.ref.repo,
                )
        )).subscribe(
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
        forkJoin(
            this._nodes.map(n =>
                this.nodeApi
                    .getNodeMetadata(n.ref.id, [RestConstants.ALL], n.ref.repo)
            )
        ).subscribe((nodes) => {
            this.saving = false;
            this.openNodes(nodes.map(n => n.node));
        });
    }

    copyNodeIdToClipboard(node: Node) {
        UIHelper.copyToClipboard(node.ref.id);
        this.toast.toast('ADMIN.APPLICATIONS.COPIED_CLIPBOARD');
    }
}
