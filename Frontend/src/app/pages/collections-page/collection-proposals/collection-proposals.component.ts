import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild,
    inject,
    ChangeDetectionStrategy,
} from '@angular/core';
import { Node, ProposalNode } from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    ColumnType,
    InteractionType,
    ListEventInterface,
    ListItem,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    OptionsHelperDataService,
    OptionsHelperService,
    Scope,
} from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';
import {
    ManagementEvent,
    ManagementEventType,
} from '../../../features/management-dialogs/management-dialogs.component';
import { MainNavService } from '../../../main/navigation/main-nav.service';

@Component({
    selector: 'es-collection-proposals',
    templateUrl: 'collection-proposals.component.html',
    styleUrls: ['collection-proposals.component.scss'],
    providers: [OptionsHelperDataService],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class CollectionProposalsComponent implements OnChanges {
    private collectionService = inject(RestCollectionService);
    private mainNavService = inject(MainNavService);
    private optionsHelperService = inject(OptionsHelperService);

    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    proposalColumns = {
        Default: [
            new ListItem('NODE', RestConstants.CM_PROP_TITLE),
            new ListItem('NODE_PROPOSAL', RestConstants.CM_CREATOR, { showLabel: false }),
            new ListItem('NODE_PROPOSAL', RestConstants.CM_PROP_C_CREATED, { showLabel: false }),
        ],
    } as ColumnType;
    // declared before the list, so it is known when the list appears
    @ViewChild(ActionbarComponent) actionbar: ActionbarComponent;
    listProposals: ListEventInterface<ProposalNode>;
    /**
     * The list is only rendered while there are proposals, so its options are set up whenever it
     * appears.
     */
    @ViewChild('listProposals') set listProposalsRef(list: ListEventInterface<ProposalNode>) {
        if (list && list !== this.listProposals) {
            void list.initOptionsGenerator({ actionbar: this.actionbar });
        }
        this.listProposals = list;
    }
    dataSourceCollectionProposals = new NodeDataSource<ProposalNode>();

    @Input() collection: Node;
    @Input() canEdit: boolean;
    /** how the proposals are listed, following the view of the collection content */
    @Input() displayType = NodeEntriesDisplayType.Grid;

    @Output() contentClick = new EventEmitter<NodeClickEvent<ProposalNode>>();
    @Output() contentDblClick = new EventEmitter<NodeClickEvent<ProposalNode>>();
    /** the proposals that are selected after each change of the selection */
    @Output() selectionChange = new EventEmitter<ProposalNode[]>();

    constructor() {
        this.mainNavService.getDialogs().eventTriggered.subscribe((event: ManagementEvent) => {
            if (event.event === ManagementEventType.AddCollectionNodes) {
                this.refreshProposals();
            }
        });
    }
    private refreshProposals() {
        this.dataSourceCollectionProposals.reset();
        this.dataSourceCollectionProposals.isLoading = true;
        if (this.canEdit) {
            this.collectionService
                .getCollectionProposals(this.collection.ref.id)
                .subscribe((proposals) => {
                    proposals.nodes = proposals.nodes.map((p) => {
                        p.proposalCollection = this.collection;
                        return p;
                    });
                    this.dataSourceCollectionProposals.setData(
                        proposals.nodes,
                        proposals.pagination,
                    );
                    this.dataSourceCollectionProposals.isLoading = false;
                });
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.collection?.currentValue !== null && changes.canEdit?.currentValue !== null) {
            this.refreshProposals();
        }
    }
}
