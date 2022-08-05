import { AfterViewInit, Component, TemplateRef, ViewChild } from '@angular/core';
import {
    NodeEntriesGlobalService,
    PaginationStrategy,
} from '../../features/node-entries/node-entries-global.service';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { ConfigService, Node } from 'ngx-edu-sharing-api';
import { EditorialDeskService } from '../editorial-desk/editorial-desk.service';
import { Scope } from '../../core-ui-module/option-item';

@Component({
    selector: 'es-custom-global-extensions',
    templateUrl: './custom-global-extensions.component.html',
    styleUrls: ['./custom-global-extensions.component.scss'],
})
export class CustomGlobalExtensionsComponent implements AfterViewInit {
    @ViewChild('titleRef') titleRef: TemplateRef<unknown>;
    @ViewChild('virtualBuffetRef') virtualBuffetRef: TemplateRef<unknown>;
    @ViewChild('virtualPublicRef') virtualPublicRef: TemplateRef<unknown>;
    @ViewChild('virtualSourceRef') virtualSourceRef: TemplateRef<unknown>;
    @ViewChild('virtualCollectionProposal') virtualCollectionProposal: TemplateRef<unknown>;
    @ViewChild('virtualCollectionProposalTaxonid')
    virtualCollectionProposalTaxonid: TemplateRef<unknown>;
    @ViewChild('virtualInCollections') virtualInCollections: TemplateRef<unknown>;
    private variables: {
        [key: string]: string;
    };

    constructor(
        private nodeEntriesGlobalService: NodeEntriesGlobalService,
        private configApi: ConfigService,
    ) {
        this.configApi.observeVariables().subscribe((v) => (this.variables = v));
    }

    ngAfterViewInit(): void {
        this.nodeEntriesGlobalService.setPaginationStrategy(
            Scope.CollectionsCollection,
            PaginationStrategy.InfiniteScroll,
        );
        this.nodeEntriesGlobalService.setPaginationStrategy(
            Scope.CollectionsReferences,
            PaginationStrategy.InfiniteScroll,
        );
        this.nodeEntriesGlobalService.setPaginationStrategy(
            Scope.WorkspaceList,
            PaginationStrategy.Paginator,
        );
        this.nodeEntriesGlobalService.setPaginationStrategy(
            Scope.Search,
            PaginationStrategy.Paginator,
        );
        this.nodeEntriesGlobalService.setPaginationStrategy(
            'DEFAULT',
            PaginationStrategy.Paginator,
        );
        this.nodeEntriesGlobalService.setPaginatorSizeOptions(
            'DEFAULT',
            [25, 50, 75, 100, 150, 200],
        );
        this.nodeEntriesGlobalService.registerCustomFieldRendering({
            type: 'NODE',
            name: RestConstants.CM_PROP_TITLE,
            templateRef: this.titleRef,
        });
        this.nodeEntriesGlobalService.registerCustomFieldRendering({
            type: 'NODE',
            name: RestConstants.LOM_PROP_TITLE,
            templateRef: this.titleRef,
        });
        this.nodeEntriesGlobalService.registerCustomFieldRendering({
            type: 'NODE',
            name: 'virtual:buffet',
            templateRef: this.virtualBuffetRef,
        });
        this.nodeEntriesGlobalService.registerCustomFieldRendering({
            type: 'NODE',
            name: 'virtual:public',
            templateRef: this.virtualPublicRef,
        });
        this.nodeEntriesGlobalService.registerCustomFieldRendering({
            type: 'NODE',
            name: 'virtual:source',
            templateRef: this.virtualSourceRef,
        });
        this.nodeEntriesGlobalService.registerCustomFieldRendering({
            type: 'NODE',
            name: 'virtual:collection_proposal',
            templateRef: this.virtualCollectionProposal,
        });
        this.nodeEntriesGlobalService.registerCustomFieldRendering({
            type: 'NODE',
            name: 'virtual:collection_proposal_taxonid',
            templateRef: this.virtualCollectionProposal,
        });
        this.nodeEntriesGlobalService.registerCustomFieldRendering({
            type: 'NODE',
            name: 'virtual:in_collections',
            templateRef: this.virtualInCollections,
        });
    }

    getBuffetType(node: Node): 'rejected' | 'check' | 'search' | 'buffet' {
        const inBuffet = node.usedInCollections.some(
            (u) =>
                (u as any).relationType === 'Usage' &&
                ((u as Node).ref.id === EditorialDeskService.BUFFET_COLLECTION_ID ||
                    (u as Node).ref.id === EditorialDeskService.BUFFET_COLLECTION_ID_STAGING),
        );
        const inEditorialCollection = node.usedInCollections.some(
            (u) =>
                (u as any).relationType === 'Usage' &&
                u.collection.type === RestConstants.COLLECTIONTYPE_EDITORIAL,
        );
        if (inBuffet) {
            return 'buffet';
        }
        if (node.isPublic || inEditorialCollection) {
            return 'search';
        }
        if (node.properties['ccm:editorial_checklist']?.[0] === '3') {
            return 'rejected';
        }
        return 'check';
    }

    getUsedInCollections(node: Node) {
        // TODO: Migrate to CollectionRelationReference type!
        return node.usedInCollections.filter((u) => (u as any).relationType === 'Usage');
    }

    getUsedInCollectionsProposals(node: Node) {
        // TODO: Migrate to CollectionRelationReference type!
        return node.usedInCollections.filter((u) => (u as any).relationType === 'Proposal');
    }

    isCollection(node: Node) {
        return (
            node?.type === RestConstants.CCM_TYPE_MAP &&
            node?.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION)
        );
    }

    getDepth(node: Node) {
        if (!this.isCollection(node)) {
            return [];
        }
        // find the relevant parent collection
        const index = Math.max(
            ...[
                EditorialDeskService.MAIN_COLLECTION_ID,
                EditorialDeskService.MAIN_COLLECTION_ID_COMMUNITIES,
            ].map((c) => node?.properties['virtual:oeh_path']?.indexOf(c)),
        );
        return node?.properties['virtual:oeh_path']?.slice(index + 1);
    }

    replaceWordpressUrl(url: string) {
        return url?.replace('https://wirlernenonline.de', this.variables?.wordpressUrl);
    }
}
