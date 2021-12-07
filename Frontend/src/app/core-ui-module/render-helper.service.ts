import { ComponentFactoryResolver, Injectable, ViewContainerRef } from '@angular/core';
import { Router } from '@angular/router';
import { MdsEditorWrapperComponent } from '../common/ui/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { replaceElementWithDiv } from '../common/ui/mds-editor/util/replace-element-with-div';
import { Node } from '../core-module/rest/data-object';
import { RestConstants } from '../core-module/rest/rest-constants';
import { RestUsageService } from '../core-module/rest/services/rest-usage.service';
import { ListItem } from '../core-module/ui/list-item';
import { CommentsListComponent } from '../modules/management-dialogs/node-comments/comments-list/comments-list.component';
import { ListTableComponent } from './components/list-table/list-table.component';
import { SpinnerComponent } from './components/spinner/spinner.component';
import { UIHelper } from './ui-helper';

@Injectable()
export class RenderHelperService {
    private static isCollectionRef(node: Node) {
        return node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1;
    }

    private viewContainerRef: ViewContainerRef;

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        private usageApi: RestUsageService,
        private router: Router,
    ) {}

    setViewContainerRef(viewContainerRef: ViewContainerRef) {
        this.viewContainerRef = viewContainerRef;
    }

    injectModuleComments(node: Node) {
        const data = {
            node,
        };
        UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            CommentsListComponent,
            document.getElementsByTagName('comments')[0],
            data,
        );
    }

    injectModuleInCollections(node: Node) {
        let domContainer: Element;
        let domCollections: Element;
        try {
            domContainer =
                document.getElementsByClassName('node_collections_render')[0].parentElement;
            domCollections = document.getElementsByTagName('collections')[0];
        } catch (e) {
            return;
        }
        domCollections = replaceElementWithDiv(domCollections);
        UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            SpinnerComponent,
            domCollections,
        );
        this.usageApi
            .getNodeUsagesCollection(
                RenderHelperService.isCollectionRef(node)
                    ? node.properties[RestConstants.CCM_PROP_IO_ORIGINAL]
                    : node.ref.id,
                node.ref.repo,
            )
            .subscribe(
                (usages) => {
                    usages = usages.filter((u) => u.collectionUsageType === 'ACTIVE');
                    // @TODO: This does currently ignore the "hideIfEmpty" flag of the mds template
                    if (usages.length === 0) {
                        domContainer.parentElement.removeChild(domContainer);
                        return;
                    }
                    const data = {
                        nodes: usages.map((u) => u.collection),
                        columns: ListItem.getCollectionDefaults(),
                        isClickable: true,
                        clickRow: (event: { node: Node }) => {
                            UIHelper.goToCollection(this.router, event.node);
                        },
                        doubleClickRow: (event: Node) => {
                            UIHelper.goToCollection(this.router, event);
                        },
                        viewType: ListTableComponent.VIEW_TYPE_GRID_SMALL,
                    };
                    UIHelper.injectAngularComponent(
                        this.componentFactoryResolver,
                        this.viewContainerRef,
                        ListTableComponent,
                        domCollections,
                        data,
                        { delay: 250 },
                    );
                },
                (error) => {
                    domContainer.parentElement.removeChild(domContainer);
                },
            );
    }

    injectMetadataEditor(node: Node) {
        const metadata = document.querySelector('.edusharing_rendering_metadata_body');
        const parent = metadata.parentElement;
        parent.removeChild(metadata);
        UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            MdsEditorWrapperComponent,
            parent,
            {
                groupId: 'io_render',
                nodes: [node],
                editorMode: 'inline',
                embedded: true,
            },
        );
    }
}
