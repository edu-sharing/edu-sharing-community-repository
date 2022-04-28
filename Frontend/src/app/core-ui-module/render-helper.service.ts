import { ComponentFactoryResolver, Injectable, ViewContainerRef } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MdsEditorWrapperComponent } from '../common/ui/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { replaceElementWithDiv } from '../common/ui/mds-editor/util/replace-element-with-div';
import { Node } from '../core-module/rest/data-object';
import { RestConstants } from '../core-module/rest/rest-constants';
import { RestUsageService } from '../core-module/rest/services/rest-usage.service';
import { ListItem } from '../core-module/ui/list-item';
import { CommentsListComponent } from '../modules/management-dialogs/node-comments/comments-list/comments-list.component';
import { NodeEntriesDisplayType } from './components/node-entries-wrapper/entries-model';
import { NodeDataSource } from './components/node-entries-wrapper/node-data-source';
import { NodeEntriesWrapperComponent } from './components/node-entries-wrapper/node-entries-wrapper.component';
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
        this.getCollectionsContainingNode(node).subscribe(
            (collections) => {
                // @TODO: This does currently ignore the "hideIfEmpty" flag of the mds template
                if (collections.length === 0) {
                    domContainer.parentElement.removeChild(domContainer);
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
                domContainer.parentElement.removeChild(domContainer);
            },
        );
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
