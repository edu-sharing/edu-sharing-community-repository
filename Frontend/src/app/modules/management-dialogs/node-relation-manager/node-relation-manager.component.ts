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
    RelationData,
    UserService
} from 'ngx-edu-sharing-api';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {NodeHelperService} from '../../../core-ui-module/node-helper.service';
import {BridgeService} from '../../../core-bridge-module/bridge.service';
import {OPEN_URL_MODE} from '../../../core-module/ui/ui-constants';
import {UniversalNode} from '../../../common/definitions';
import { forkJoin } from 'rxjs';
import {Toast} from '../../../core-ui-module/toast';
import {first} from 'rxjs/internal/operators';

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
    swapRelation: boolean;
    @Input() set nodes(nodes: Node[]) {
        this._nodes = nodes;
        this.source = nodes[0];
        this.relationService.getRelations(
            this._nodes[0].ref.id
        ).subscribe((relations) => {
            this.relations = relations.relations;
            this.loading = false;
        },e => {
               this.close.emit(false);
            });
    }
    @Output() close = new EventEmitter<boolean>();

    readonly form = new FormGroup({
        relation: new FormControl(Relations.isBasedOn, Validators.required),
    });
    readonly buttons = [new DialogButton('CLOSE',
        DialogButton.TYPE_CANCEL,
        () => this.close.emit(false)
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
    loading = true;

    constructor(
        private relationService: RelationService,
        private nodeHelper: NodeHelperService,
        private userService: UserService,
        private toast: Toast,
        private bridgeService: BridgeService,
    ) {
    }

    ngOnInit() {
        this.updateButtons();
    }

    getRelationKeys() {
        return [...new Set(this.addRelations.concat(this.relations || [])?.map(r => r.type))].sort();
    }


    swap() {
        this.swapRelation = !this.swapRelation;
        /*const tmp = this.target;
        this.target = this.source;
        this.source = tmp;*/
    }

    getCriterias(): SearchRequestCriteria[] {
        return [{
            property: "sourceNode",
            values: [this._nodes[0].ref.id]
        }];
    }
    getAllExistingRelations() {
        return this.getAllRelations().filter((r) => !this.deleteRelations.includes(r));
    }

    getAllRelations() {
        return this.relations.concat(this.addRelations);
    }

    getRelations(key: 'isPartOf' | 'isBasedOn' | 'references' | 'hasPart' | 'isBasisFor'): RelationData[] {
        return this.getAllRelations().filter(r => r.type === key).sort((a,b) => a.timestamp > b.timestamp ? 1 : -1);
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
    resolveRelationSendData(r: RelationData) {
        const inverted = this.isInverted(r);
        let source = this.source.ref.id;
        let target = r.node.ref.id;
        let type: string = r.type;
        if(inverted) {
            source = r.node.ref.id;
            target = this.source.ref.id;
            type = inverted;
        }
        return {
            source,
            target,
            type
        }
    }
    private async save() {
        this.toast.showProgressDialog();
        try {
            await forkJoin(this.addRelations.map(r => {
                const data = this.resolveRelationSendData(r);
                return this.relationService.createRelation(
                    data.source,
                    data.target,
                    data.type as any
                )
            })).toPromise();
            await forkJoin(this.deleteRelations.map(r => {
                const data = this.resolveRelationSendData(r);
                return this.relationService.deleteRelation(
                    data.source,
                    data.target,
                    data.type as any
                )
            })).toPromise();
            this.close.emit(true);
        } catch(e) {
        }
        this.toast.closeModalDialog();
    }

    private updateButtons() {
        this.buttons[1].disabled = !this.addRelations.length && !this.deleteRelations.length;
    }
    getCurrentType() {

    }
    async createRelation() {
        let type = this.form.get('relation').value;
        if(this.swapRelation) {
            type = (this.RelationsInverted as any)[type];
        }
        if(this.getAllExistingRelations().find((r) =>
            r.node.ref.id === this.target.ref.id && r.type === type
        )) {
            this.toast.error(null, 'NODE_RELATIONS.RELATION_EXISTS');
            return;
        }
        this.addRelations.push({
            node: this.target,
            type,
            // @TODO: check if api model is invalid
            timestamp: new Date().getTime() as any,
            creator: (await this.userService.observeCurrentUser().pipe(first()).toPromise()).person
        });
        this.form.reset();
        this.form.setValue({relation: Relations.isBasedOn});
        this.swapRelation = false;
        this.updateButtons();
        this.target = null;
    }

    private isInverted(r: RelationData) {
        return Object.keys(this.RelationsInverted).find(k => (this.RelationsInverted as any)[k] === r.type && k !== r.type)
    }
}
export enum Relations {
    isBasedOn = 'isBasedOn',
    isPartOf = 'isPartOf',
    references = 'references'
}
