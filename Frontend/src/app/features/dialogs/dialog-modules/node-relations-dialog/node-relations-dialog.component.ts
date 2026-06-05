import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    inject,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import {
    Node,
    NodeService,
    NodeRelationData,
    RelationService,
    UserService,
    ConfigService,
} from 'ngx-edu-sharing-api';
import { forkJoin } from 'rxjs';
import { first } from 'rxjs/operators';
import { UniversalNode } from '../../../../core-module/rest/definitions';
import { BridgeService } from '../../../../services/bridge.service';
import {
    DialogButton,
    RestConstants,
    RestHelper,
    SearchRequestCriteria,
} from '../../../../core-module/core.module';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { Toast } from '../../../../services/toast';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { NodeRelationsDialogData, NodeRelationsDialogResult } from './node-relations-dialog-data';
import {
    ColumnType,
    ListItem,
    LocalEventsService,
    NodesRightMode,
    OPEN_URL_MODE,
} from 'ngx-edu-sharing-ui';

enum Relations {
    isPartOf = 'isPartOf',
    isBasedOn = 'isBasedOn',
    references = 'references',
    isDuplicateOf = 'isDuplicateOf',
    requires = 'requires',
    replaces = 'replaces',
    hasFormat = 'hasFormat',
}

@Component({
    selector: 'es-node-relations-dialog',
    templateUrl: './node-relations-dialog.component.html',
    styleUrls: ['./node-relations-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class NodeRelationsDialogComponent implements OnInit {
    data = inject<NodeRelationsDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<NodeRelationsDialogData, NodeRelationsDialogResult>>(CardDialogRef);
    private bridgeService = inject(BridgeService);
    private cdr = inject(ChangeDetectorRef);
    private localEvents = inject(LocalEventsService);
    private nodeHelper = inject(NodeHelperService);
    private nodeService = inject(NodeService);
    private relationService = inject(RelationService);
    private toast = inject(Toast);
    private userService = inject(UserService);
    private configService = inject(ConfigService);

    readonly RelationsInverted: { [key: string]: NodeRelationData['reverseType'] } = {
        [Relations.isPartOf]: 'hasPart',
        [Relations.isBasedOn]: 'isBasisFor',
        [Relations.references]: 'references',
        [Relations.isDuplicateOf]: 'isDuplicateOf',
        [Relations.requires]: 'isRequiredBy',
        [Relations.replaces]: 'isReplacedBy',
        [Relations.hasFormat]: 'isFormatOf',
    };

    source: UniversalNode;
    relations: NodeRelationData[];
    addRelations: NodeRelationData[] = [];
    deleteRelations: NodeRelationData[] = [];
    swapRelation: boolean;
    readonly form = new UntypedFormGroup({
        relation: new UntypedFormControl(Relations.isBasedOn, Validators.required),
    });
    permissions = [RestConstants.PERMISSION_WRITE];
    target: UniversalNode;
    columns = { Default: [new ListItem('NODE', RestConstants.LOM_PROP_TITLE)] } as ColumnType;
    allowedRelations: string[];

    private readonly buttons = [
        new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.dialogRef.close(null)),
        new DialogButton('SAVE', DialogButton.TYPE_PRIMARY, () => this.save()),
    ];

    constructor() {
        this.dialogRef.patchState({ isLoading: true });
    }

    async ngOnInit(): Promise<void> {
        this.allowedRelations =
            (await this.configService.get<string[]>('relations.allowedRelations')) ??
            Object.values(Relations);
        this.dialogRef.patchConfig({ buttons: this.buttons });
        void this.initNode(this.data.node);
        this.updateButtons();
    }

    getRelationKeys() {
        return [
            ...new Set(this.addRelations.concat(this.relations || [])?.map((r) => r.type)),
        ].sort();
    }

    swap() {
        this.swapRelation = !this.swapRelation;
    }

    getCriteria(): SearchRequestCriteria[] {
        return [
            {
                property: 'sourceNode',
                values: [this.source.ref.id],
            },
        ];
    }

    getAllExistingRelations() {
        return this.getAllRelations().filter((r) => !this.deleteRelations.includes(r));
    }

    getAllRelations() {
        return this.relations.concat(this.addRelations);
    }

    getRelations(key: NodeRelationData['type']): NodeRelationData[] {
        return this.getAllRelations()
            .filter((r) => r.type === key)
            .sort((a, b) => (a.createdAt > b.createdAt ? 1 : -1));
    }

    openNode(node: UniversalNode) {
        UIHelper.openUrl(
            this.nodeHelper.getNodeUrl(node, {
                closeOnBack: true,
            }),
            this.bridgeService,
            OPEN_URL_MODE.Blank,
        );
    }

    removeRelation(relation: NodeRelationData) {
        if (!this.deleteRelations.includes(relation)) {
            this.deleteRelations.push(relation);
        }
        this.updateButtons();
    }

    resolveRelationSendData(r: NodeRelationData) {
        const inverted = this.isInverted(r);
        let source = this.source.ref.id;
        let target = r.toNode.ref.id;
        let type: string = r.type;
        if (inverted) {
            source = r.toNode.ref.id;
            target = this.source.ref.id;
            type = inverted;
        }
        return {
            source,
            target,
            type,
        };
    }

    private async save() {
        this.toast.showProgressSpinner();
        try {
            await forkJoin(
                this.addRelations.map((r) => {
                    const data = this.resolveRelationSendData(r);
                    return this.relationService.createRelation(
                        data.source,
                        data.target,
                        data.type as any,
                    );
                }),
            ).toPromise();
            await forkJoin(
                this.deleteRelations.map((r) => {
                    const data = this.resolveRelationSendData(r);
                    return this.relationService.deleteRelation(
                        data.source,
                        data.target,
                        data.type as any,
                    );
                }),
            ).toPromise();
            this.dialogRef.close(true);
            this.localEvents.nodesChanged.emit([this.data.node]);
        } catch (e) {}
        this.toast.closeProgressSpinner();
    }

    updateButtons() {
        const hasChanges = this.hasChanges();
        this.buttons[1].disabled = !hasChanges;
        if (hasChanges) {
            this.dialogRef.patchConfig({ closable: Closable.Confirm });
        } else if (this.target) {
            this.dialogRef.patchConfig({ closable: Closable.Standard });
        } else {
            this.dialogRef.patchConfig({ closable: Closable.Casual });
        }
    }

    getCurrentType() {}

    async createRelation() {
        let type = this.form.get('relation').value;
        if (this.swapRelation) {
            type = (this.RelationsInverted as any)[type];
        }
        if (
            this.getAllExistingRelations().find(
                (r) => r.toNode.ref.id === this.target.ref.id && r.type === type,
            )
        ) {
            this.toast.error(null, 'NODE_RELATIONS.RELATION_EXISTS');
            return;
        }
        this.addRelations.push({
            fromNode: this.source,
            toNode: this.target,
            type: type,
            reverseType: this.RelationsInverted[type],
            // @TODO: check if api model is invalid
            createdAt: new Date().getTime() as any,
            createdBy: (await this.userService.observeCurrentUser().pipe(first()).toPromise())
                .person,
            isAiGenerated: false,
            evaluation: {
                isApproved: true,
            },
            metadata: {},
        });
        this.form.reset();
        this.form.setValue({ relation: Relations.isBasedOn });
        this.swapRelation = false;
        this.target = null;
        this.updateButtons();
    }

    private isInverted(r: NodeRelationData) {
        return Object.keys(this.RelationsInverted).find(
            (k) => (this.RelationsInverted as any)[k] === r.type && k !== r.type,
        );
    }

    isPublishedCopy() {
        return !!this.source.properties[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL]?.[0];
    }

    isSwappable() {
        const relation = this.form.get('relation').value;
        return !((this.RelationsInverted as any)[relation] === relation);
    }

    canModify(relation: NodeRelationData) {
        return this.nodeHelper.getNodesRight(
            [relation.toNode],
            RestConstants.PERMISSION_WRITE,
            NodesRightMode.Effective,
        );
    }

    private hasChanges() {
        return this.addRelations?.length || this.deleteRelations?.length;
    }

    private async initNode(node: Node): Promise<void> {
        // published original: we now need to switch to the original id!
        if (node.properties[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL]) {
            // switch to original node id!
            node = await this.nodeService
                .getNode(
                    RestHelper.removeSpacesStoreRef(
                        node.properties[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL][0],
                    ),
                )
                .toPromise();
        }
        this.source = node;
        this.relationService.getRelations(node.ref.id).subscribe(
            (relations) => {
                this.relations = relations;
                this.dialogRef.patchState({ isLoading: false });
                this.cdr.detectChanges();
            },
            (e) => {
                this.dialogRef.close(null);
            },
        );
    }
}
