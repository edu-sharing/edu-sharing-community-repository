import {
    Component, ContentChild,
    EventEmitter,
    HostListener,
    Input,
    OnInit,
    Output, TemplateRef,
} from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    ListItem,
    Node, RequestObject,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestIamService,
    RestNodeService,
} from '../../../core-module/core.module';
import { Toast } from '../../toast';
import { OptionItem } from '../../option-item';

/**
 * An edu-sharing sidebar dialog for adding data to a collection
 */
@Component({
    selector: 'es-collection-chooser',
    templateUrl: 'collection-chooser.component.html',
    styleUrls: ['collection-chooser.component.scss'],
})
export class CollectionChooserComponent implements OnInit {
    @ContentChild('beforeRecent') beforeRecentRef: TemplateRef<any>;
    /**
     * The caption of the dialog, will be translated automatically
     */
    @Input() title: string;

    /**
     * Fired when an element is choosen, a (collection) Node will be send as a result
     */
    @Output() onChoose = new EventEmitter();
    @Output() onCreateCollection = new EventEmitter();
    /**
     * Fired when a list of nodes is dropped on a collection item
     */
    @Output() onDrop = new EventEmitter();
    /**
     * Fired when the dialog should be closed
     */
    @Output() onCancel = new EventEmitter();

    searchQuery = '';
    lastSearchQuery = '';
    isLoadingLatest = true;
    isLoadingMy = true;
    currentRoot: Node;
    breadcrumbs: Node[];
    listLatest: Node[];
    listMy: Node[];
    createCollectionOptionItem = new OptionItem(
        'OPTIONS.NEW_COLLECTION',
        'add',
        () => this.createCollection(),
    );

    private hasMoreToLoad: boolean;
    columns: ListItem[] = ListItem.getCollectionDefaults();
    sortBy: string[];
    sortAscending = false;
    /**
     * shall more than 5 recent collections be shown
     */
    showMore = false;
    canCreate = false;

    constructor(
        private connector: RestConnectorService,
        private router: Router,
        private iam: RestIamService,
        private collectionApi: RestCollectionService,
        private node: RestNodeService,
        private toast: Toast,
        private translate: TranslateService,
    ) {
        // http://plnkr.co/edit/btpW3l0jr5beJVjohy1Q?p=preview
        this.sortBy = [RestConstants.CM_MODIFIED_DATE];
        this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS).subscribe((tp) => this.canCreate = tp);
    }

    ngOnInit(): void {
        this.loadLatest(true);
        this.loadMy();
    }

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.code == 'Escape') {
            event.preventDefault();
            event.stopPropagation();
            this.cancel();
            return;
        }
    }

    createCollection() {
        this.onCreateCollection.emit(
            this.currentRoot ? this.currentRoot : null,
        );
    }

    loadLatest(reset = false) {
        if (reset) {
            this.listLatest = [];
            this.lastSearchQuery = this.searchQuery;
        } else if (!this.hasMoreToLoad) {
            return;
        }
        this.isLoadingLatest = true;
        const request = {
            sortBy: this.sortBy,
            offset: this.listLatest.length,
            sortAscending: false,
            propertyFilter: [RestConstants.ALL]
        };
        let requestCall;
        if(this.lastSearchQuery) {
            requestCall = this.collectionApi.search(this.lastSearchQuery, request);
        } else {
            requestCall = this.collectionApi.getCollectionSubcollections(RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_RECENT, [], request);
        }
        requestCall.subscribe(data => {
                this.isLoadingLatest = false;
                this.hasMoreToLoad = data.collections.length > 0;
                this.showMore = !!this.lastSearchQuery;
                this.listLatest = this.listLatest.concat(data.collections);
            }, (error) => {
                this.isLoadingLatest = false;
                this.hasMoreToLoad = false;
            });
    }

    cancel() {
        this.onCancel.emit();
    }

    navigateBack() {
        if (this.breadcrumbs.length > 1)
            this.currentRoot = this.breadcrumbs[
                this.breadcrumbs.length - 2
            ] as any;
        else this.currentRoot = null;
        this.loadMy();
    }

    drop(event: any) {
        if (!this.checkPermissions(event.target)) {
            return;
        }
        this.onDrop.emit(event);
    }

    private checkPermissions(node: Node) {
        if (!this.hasWritePermissions(node)) {
            this.toast.error(null, 'NO_WRITE_PERMISSIONS');
            return false;
        }
        return true;
    }
    hasWritePermissions = (node: Node) => this.hasWritePermissionsInternal(node);

    hasWritePermissionsInternal(
        node: Node,
    ): {
        status: boolean;
        message?: string;
        button?: {
            click: () => void;
            caption: string;
            icon: string;
        };
    } {
        /*if (this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_COLLECTION_PROPOSAL)){
            return { status: false, message: 'COLLECTIONS.SUGGEST_ONLY', button: {
                    click: () => this.onChoose.emit(node),
                    caption: 'TEST',
                    icon: 'home'
                } };
        }*/
        if (!this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_COLLECTION_PROPOSAL) &&
            node.access.indexOf(RestConstants.ACCESS_WRITE) === -1) {
            return { status: false, message: 'NO_WRITE_PERMISSIONS' };
        }
        return { status: true };
    }

    goIntoCollection(node: Node) {
        this.currentRoot = node;
        this.loadMy();
    }

    clickCollection(node: Node) {
        if (!this.checkPermissions(node)) {
            return;
        }
        this.onChoose.emit(node);
    }

    private loadMy() {
        this.listMy = [];
        this.breadcrumbs = null;
        this.isLoadingMy = true;
        this.collectionApi
            .getCollectionSubcollections(
                this.currentRoot ? this.currentRoot.ref.id : RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_MY,
                [RestConstants.ALL],
                {
                    sortBy: this.sortBy,
                    sortAscending: false,
                    count: RestConstants.COUNT_UNLIMITED,
                },
            )
            .subscribe(data => {
                this.isLoadingMy = false;
                this.listMy = this.listMy.concat(data.collections);
            });
        if (this.currentRoot) {
            this.node
                .getNodeParents(this.currentRoot.ref.id, false)
                .subscribe(list => (this.breadcrumbs = list.nodes.reverse()));
        }
    }
}
