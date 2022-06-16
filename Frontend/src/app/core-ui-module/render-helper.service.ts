import {ComponentFactoryResolver, Injectable, ViewContainerRef} from '@angular/core';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {Node} from '../core-module/rest/data-object';
import {RestConstants} from '../core-module/rest/rest-constants';
import {RestUsageService} from '../core-module/rest/services/rest-usage.service';
import {ListItem} from '../core-module/ui/list-item';
import { CommentsListComponent } from '../modules/management-dialogs/node-comments/comments-list/comments-list.component';
import {SpinnerComponent} from '../shared/components/spinner/spinner.component';
import {UIHelper} from './ui-helper';
import {
    MdsNodeRelationsWidgetComponent
} from '../common/ui/node-render/node-relations/node-relations-widget.component';
import { replaceElementWithDiv } from '../features/mds/mds-editor/util/replace-element-with-div';
import { MdsEditorWrapperComponent } from '../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { NodeEntriesDisplayType } from '../features/node-entries/entries-model';
import { NodeDataSource } from '../features/node-entries/node-data-source';
import { NodeEntriesWrapperComponent } from '../features/node-entries/node-entries-wrapper.component';

@Injectable()
export class RenderHelperService {
    private static isCollectionRef(node: Node) {
        return node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1;
    }

    private viewContainerRef: ViewContainerRef;

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        private usageApi: RestUsageService,
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
        let domCollections: Element;
        let parent: Element;
        try {
            domCollections =
                document.evaluate('//*[@id="edusharing_rendering_metadata"]//collections',
                    document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue as Element
            parent = domCollections.parentElement.parentElement;
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
        this.getCollectionsContainingNode(node).subscribe(
            (collections) => {
                // @TODO: This does currently ignore the "hideIfEmpty" flag of the mds template
                if (collections.length === 0) {
                    parent.remove();
                    return;
                }
                const data = {
                    dataSource: new NodeDataSource(collections),
                    columns: ListItem.getCollectionDefaults(),
                    displayType: NodeEntriesDisplayType.SmallGrid,
                };
                const entriesComponentRef = UIHelper.injectAngularComponent(
                    this.componentFactoryResolver,
                    this.viewContainerRef,
                    NodeEntriesWrapperComponent,
                    domCollections,
                    data,
                    { delay: 250 },
                );
                entriesComponentRef.instance.ngOnChanges();
            },
            (error) => {
                parent.remove();
            },
        );
    }
    injectNodeRelationsWidget(node: Node) {
        let domRelations;
        try {
            domRelations =
                document.evaluate('//*[@id="edusharing_rendering_metadata"]//nodeRelations',
                    document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue as Element
        } catch (e) { }
        if(domRelations) {
            domRelations = replaceElementWithDiv(domRelations);
            const component = UIHelper.injectAngularComponent(
                this.componentFactoryResolver,
                this.viewContainerRef,
                MdsNodeRelationsWidgetComponent,
                domRelations,
                {
                    node
                },
            );
            component.instance.ngOnChanges();
            return component;
        }
        return null;
    }

    injectMetadataEditor(node: Node, groupId = 'io_render') {
        const metadata = document.querySelector('.edusharing_rendering_metadata_body');
        const parent = metadata.parentElement;
        parent.removeChild(metadata);
        const component = UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            MdsEditorWrapperComponent,
            parent,
            {
                groupId,
                nodes: [node],
                editorMode: 'inline',
                embedded: true,
            },
        );
        // enforce to render all widgets, since rendering does not support extended state
        component.instance.getInstanceService().shouldShowExtendedWidgets$.next(true);
        return component;
    }

    private getCollectionsContainingNode(node: Node): Observable<Node[]> {
        const id = this.getOriginalId(node);
        return this.usageApi.getNodeUsagesCollection(id, node.ref.repo).pipe(
            map((usages) => usages.filter((usage) => usage.collectionUsageType === 'ACTIVE')),
            map((usages) => usages.map((usage) => usage.collection)),
        );
    }

    private getOriginalId(node: Node): string {
        if (RenderHelperService.isCollectionRef(node)) {
            return node.properties[RestConstants.CCM_PROP_IO_ORIGINAL];
        } else {
            return node.ref.id;
        }
    }
}
