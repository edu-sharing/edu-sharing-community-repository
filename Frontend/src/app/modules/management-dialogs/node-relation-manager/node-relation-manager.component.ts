import {trigger} from '@angular/animations';
import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output,} from '@angular/core';
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
    RelationV1Service
} from '../../../../../projects/edu-sharing-api/src/lib/api/services/relation-v-1.service';
import {
    RelationData
} from '../../../../../projects/edu-sharing-api/src/lib/api/models/relation-data';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {NodeHelperService} from '../../../core-ui-module/node-helper.service';
import {BridgeService} from '../../../core-bridge-module/bridge.service';
import {OPEN_URL_MODE} from '../../../core-module/ui/ui-constants';

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
export class NodeRelationManagerComponent {
    readonly Relations = Object.values(Relations);
    readonly RelationsInverted = {
        [Relations.isBasedOn]: 'isBasisFor',
        [Relations.isPartOf]: 'hasPart',
        [Relations.references]: 'references'
    };
    source: Node;
    _nodes: Node[];
    relations: RelationData[];
    @Input() set nodes(nodes: Node[]) {
        this._nodes = nodes;
        this.source = nodes[0];
        this.relationService.getRelations({
            repository: RestConstants.HOME_REPOSITORY,
            node: this._nodes[0].ref.id
        }).subscribe((relations) =>
            this.relations = relations.relations
        , (e: any) => {
            // @TODO
            this.relations = [
                {
                    node: this._nodes[0] as any,
                    type: 'isPartOf',
                    timestamp: '' + (new Date().getTime() - Math.random() * 100000000)
                },
                {
                    node: this._nodes[0] as any,
                    type: 'isPartOf',
                    timestamp: '' + (new Date().getTime() - Math.random() * 100000000)
                },
                {
                    node: this._nodes[0] as any,
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
    readonly buttons = DialogButton.getSingleButton('CLOSE',
        () => this.onClose.emit(),
        DialogButton.TYPE_CANCEL);
    permissions = [RestConstants.PERMISSION_WRITE];
    target: Node;
    columns = [
        new ListItem('NODE', RestConstants.LOM_PROP_TITLE)
    ];

    constructor(
        private relationService: RelationV1Service,
        private nodeHelper: NodeHelperService,
        private bridgeService: BridgeService,
    ) {
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

    getRelations(key: 'isPartOf' | 'isBasedOn' | 'references' | 'hasPart' | 'isBaseFor'): RelationData[] {
        return this.relations.filter(r => r.type === key).sort((a,b) => a.timestamp.localeCompare(b.timestamp));
    }

    openNode(node: Node) {
        UIHelper.openUrl(
            this.nodeHelper.getNodeUrl(node),
            this.bridgeService,
            OPEN_URL_MODE.Blank
        );
    }

    removeRelation(relation: RelationData) {

    }
}
export enum Relations {
    isBasedOn = 'isBasedOn',
    isPartOf = 'isPartOf',
    references = 'references'
}
