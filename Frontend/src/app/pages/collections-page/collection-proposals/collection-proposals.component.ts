import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild,
    inject,
} from '@angular/core';
import { Node, ProposalNode } from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    ColumnType,
    InteractionType,
    ListEventInterface,
    ListItem,
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
    @ViewChild('listProposals') listProposals: ListEventInterface<ProposalNode>;
    @ViewChild(ActionbarComponent) actionbar: ActionbarComponent;
    dataSourceCollectionProposals = new NodeDataSource<ProposalNode>();

    @Input() collection: Node;
    @Input() canEdit: boolean;

    @Output() contentClick = new EventEmitter<ProposalNode>();

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
                    setTimeout(() => {
                        void this.listProposals?.initOptionsGenerator({
                            actionbar: this.actionbar,
                        });
                    });
                });
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.collection?.currentValue !== null && changes.canEdit?.currentValue !== null) {
            this.refreshProposals();
        }
    }
}
