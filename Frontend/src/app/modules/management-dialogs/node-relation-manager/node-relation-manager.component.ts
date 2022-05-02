import {trigger} from '@angular/animations';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {
    DialogButton,
    ListItem,
    Node,
    RestConstants,
    SearchRequestCriteria,
} from '../../../core-module/core.module';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {
    RelationService,
    RelationData
} from 'ngx-edu-sharing-api';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {NodeHelperService} from '../../../core-ui-module/node-helper.service';
import {BridgeService} from '../../../core-bridge-module/bridge.service';
import {OPEN_URL_MODE} from '../../../core-module/ui/ui-constants';
import {UniversalNode} from '../../../common/definitions';
import { forkJoin } from 'rxjs';
import {Toast} from '../../../core-ui-module/toast';

@Component({
    selector: 'es-node-relation-manager',
    templateUrl: 'node-relation-manager.component.html',
    styleUrls: ['node-relation-manager.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeRelationManagerComponent implements OnInit{
    readonly Relations = Object.values(Relations);
    readonly RelationsInverted = {
        [Relations.isBasedOn]: 'isBasisFor',
        [Relations.isPartOf]: 'hasPart',
        [Relations.references]: 'references'
    };
    source: UniversalNode;
    _nodes: UniversalNode[];
    relations: RelationData[];
    addRelations: RelationData[] = [];
    deleteRelations: RelationData[] = [];
    @Input() set nodes(nodes: Node[]) {
        this._nodes = nodes;
        this.source = nodes[0];
        this.relationService.getRelations(
            this._nodes[0].ref.id
        ).subscribe((relations) =>
                this.relations = relations.relations
            , (e: any) => {
                // @TODO
                this.relations = [
                    {
                        node: this._nodes[0],
                        type: 'isPartOf',
                        timestamp: '' + (new Date().getTime() - Math.random() * 100000000)
                    },
                    {
                        node: this._nodes[0],
                        type: 'isPartOf',
                        timestamp: '' + (new Date().getTime() - Math.random() * 100000000)
                    },
                    {
                        node: this._nodes[0],
                        type: 'isBasedOn',
                        timestamp: '' + (new Date().getTime() - Math.random() * 100000000)
                    },
                    {
                        node: this._nodes[0] as any,
                        type: 'references',
                        timestamp: '' + (new Date().getTime() - Math.random() * 100000000)
                    }
                ]
            });
    }
    @Output() onClose = new EventEmitter<void>();

    readonly form = new FormGroup({
        relation: new FormControl(Relations.isBasedOn, Validators.required),
    });
    readonly buttons = [new DialogButton('CLOSE',
        DialogButton.TYPE_CANCEL,
        () => this.onClose.emit()
    ),
        new DialogButton('SAVE',
            DialogButton.TYPE_PRIMARY,
            () => this.save(),
        )
    ];
    permissions = [RestConstants.PERMISSION_WRITE];
    target: UniversalNode;
    columns = [
        new ListItem('NODE', RestConstants.LOM_PROP_TITLE)
    ];

    constructor(
        private relationService: RelationService,
        private nodeHelper: NodeHelperService,
        private toast: Toast,
        private bridgeService: BridgeService,
    ) {
    }

    ngOnInit() {
        this.updateButtons();
    }

    getRelationKeys() {
        return [...new Set(this.relations?.map(r => r.type))].sort();
    }


    swap() {
        const tmp = this.target;
        this.target = this.source;
        this.source = tmp;
    }

    getCriterias(): SearchRequestCriteria[] {
        return [{
            property: "sourceNode",
            values: [this._nodes[0].ref.id]
        }];
    }

    getRelations(key: 'isPartOf' | 'isBasedOn' | 'references' | 'hasPart' | 'isBasisFor'): RelationData[] {
        return this.relations.concat(this.addRelations).filter(r => r.type === key).sort((a,b) => a.timestamp.localeCompare(b.timestamp));
    }

    openNode(node: UniversalNode) {
        UIHelper.openUrl(
            this.nodeHelper.getNodeUrl(node),
            this.bridgeService,
            OPEN_URL_MODE.Blank
        );
    }

    removeRelation(relation: RelationData) {
        if(!this.deleteRelations.includes(relation)) {
            this.deleteRelations.push(relation);
        }
        this.updateButtons();
    }

    private async save() {
        this.toast.showProgressDialog();
        try {
            await forkJoin(this.addRelations.map(r =>
                this.relationService.createRelation(
                    this.source.ref.id,
                    r.node.ref.id,
                    r.type as any
                )
            )).toPromise();
            await forkJoin(this.deleteRelations.map(r =>
                this.relationService.deleteRelation(
                    this.source.ref.id,
                    r.node.ref.id,
                    r.type as any
                )
            )).toPromise();
            this.onClose.emit();
        } catch(e) {

        }
        this.toast.closeModalDialog();
    }

    private updateButtons() {
        this.buttons[1].disabled = !this.addRelations.length || !this.deleteRelations.length;
    }

    createRelation() {
        this.addRelations.push({
            node: this.target,
            type: this.form.get('relation').value
        });
        this.form.reset();
        this.target = null;
    }
}
export enum Relations {
    isBasedOn = 'isBasedOn',
    isPartOf = 'isPartOf',
    references = 'references'
}
