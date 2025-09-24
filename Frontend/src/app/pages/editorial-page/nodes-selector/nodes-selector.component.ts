import { CommonModule } from '@angular/common';
import { Component, OnInit, signal, WritableSignal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTabChangeEvent, MatTabsModule } from '@angular/material/tabs';
import { TranslateModule } from '@ngx-translate/core';
import { HOME_REPOSITORY, Node } from 'ngx-edu-sharing-api';
import {
    ColumnType,
    EduSharingUiModule,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesService,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';
import { CollectionSubcollections } from '../../../core-module/rest/data-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';

@Component({
    selector: 'es-nodes-selector',
    templateUrl: 'nodes-selector.component.html',
    styleUrls: ['nodes-selector.component.scss'],
    imports: [CommonModule, EduSharingUiModule, MatButtonModule, MatTabsModule, TranslateModule],
    providers: [NodeEntriesService],
})
export class NodesSelectorComponent implements OnInit {
    selectedTabIndex: number = 0;

    // collections tab
    collectionsColumns: ColumnType;
    dataSourceCollections: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();
    selectedNodes: WritableSignal<Partial<Node>[]> = signal([]);

    constructor(private collectionService: RestCollectionService) {}

    /**
     * Initializes the component by definition the default columns for the collections data source.
     */
    ngOnInit(): void {
        this.collectionsColumns = {
            Default: ListItem.getCollectionDefaults(),
        };
    }

    /**
     * Listens to the selection change event to update the selected nodes.
     *
     * @param event
     */
    onCollectionSelectionChange(event: any) {
        const selectedNodes = this.selectedNodes();
        event.added?.forEach((node: Node) => {
            selectedNodes.push(node);
        });
        event.removed?.forEach((node: Node) => {
            const selectedIndex = this.selectedNodes().indexOf(node);
            if (selectedIndex !== -1) {
                selectedNodes.splice(selectedIndex, 1);
            }
        });
        this.selectedNodes.set(selectedNodes);
    }

    /**
     * Callback for the tab change.
     *
     * @param event
     */
    async onTabChange(event: MatTabChangeEvent) {
        this.selectedTabIndex = event.index;
        if (this.selectedTabIndex === 1) {
            await this.updateCollectionsDataSource();
        }
    }

    /**
     * Copies the selected nodes into the currently opened view.
     *
     * @param nodes
     */
    copyNodes(nodes: Partial<Node>[]) {
        console.log('copyNodes', nodes);
    }

    /**
     * Helper function to initialize the collections datasource with (faked) nodes for "my" and "editorial" collections.
     */
    private async updateCollectionsDataSource(): Promise<void> {
        // return, if dataSource is already initialized
        if (this.dataSourceCollections.getData()?.length) {
            return;
        }
        this.dataSourceCollections.isLoading = true;
        let initialData: Partial<Node>[] = [];
        const request = {
            sortBy: [RestConstants.CM_PROP_TITLE],
            sortAscending: true,
        };
        // my collections
        const myCollectionsNode: Partial<Node> = this.createFakeNode(
            'Meine Sammlungen',
            'person',
            RestConstants.COLLECTIONSCOPE_MY,
        );
        const subMyCollections: CollectionSubcollections = await firstValueFrom(
            this.collectionService.getCollectionSubcollections(
                RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_MY,
                [],
                request,
            ),
        );
        subMyCollections.collections?.forEach((collection) => {
            // set the ID to the (fake) parent node
            collection.parent.id = myCollectionsNode.ref.id;
        });
        initialData.push(myCollectionsNode);
        initialData = initialData.concat(subMyCollections.collections);
        // editorial collections
        const editorialCollectionsNode: Partial<Node> = this.createFakeNode(
            'Redaktionelle Sammlungen',
            'star',
            RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
        );
        const subEditorialCollections: CollectionSubcollections = await firstValueFrom(
            this.collectionService.getCollectionSubcollections(
                RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
                [],
                request,
            ),
        );
        subEditorialCollections.collections?.forEach((collection) => {
            // set the ID to the (fake) parent node
            collection.parent.id = editorialCollectionsNode.ref.id;
        });
        initialData.push(editorialCollectionsNode);
        initialData = initialData.concat(subEditorialCollections.collections);
        this.dataSourceCollections.setData(initialData);
        this.dataSourceCollections.isLoading = false;
    }

    /**
     * Helper function to create a fake node for the datasource, e.g., for the parent elements
     * of the collections tree.
     *
     * @param title
     * @param icon
     * @param scope
     */
    private createFakeNode(title: string, icon: string, scope: string): Partial<Node> {
        return {
            collection: {
                fromUser: false,
                level0: false,
                scope,
                title,
                type: '',
            },
            preview: {
                isIcon: true,
                height: 20,
                url: icon,
                width: 20,
            },
            ref: {
                archived: false,
                id: uuidv4(),
                repo: HOME_REPOSITORY,
            },
            title,
        };
    }

    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
}
