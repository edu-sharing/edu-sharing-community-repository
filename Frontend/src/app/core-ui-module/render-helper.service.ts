import { ComponentFactoryResolver, Injectable, ViewContainerRef } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Node } from '../core-module/rest/data-object';
import { RestConstants } from '../core-module/rest/rest-constants';
import { RestUsageService } from '../core-module/rest/services/rest-usage.service';
import { ListItem } from '../core-module/ui/list-item';
import { CommentsListComponent } from '../modules/management-dialogs/node-comments/comments-list/comments-list.component';
import { SpinnerComponent } from '../shared/components/spinner/spinner.component';
import { UIHelper } from './ui-helper';
import { MdsNodeRelationsWidgetComponent } from '../common/ui/node-render/node-relations/node-relations-widget.component';
import { replaceElementWithDiv } from '../features/mds/mds-editor/util/replace-element-with-div';
import { MdsEditorWrapperComponent } from '../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { NodeEntriesDisplayType } from '../features/node-entries/entries-model';
import { NodeDataSource } from '../features/node-entries/node-data-source';
import { NodeEntriesWrapperComponent } from '../features/node-entries/node-entries-wrapper.component';
import { OptionsHelperService } from './options-helper.service';
import { Scope, Target } from './option-item';

@Injectable()
export class RenderHelperService {
    private static isCollectionRef(node: Node) {
        return node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1;
    }

    private viewContainerRef: ViewContainerRef;

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        private usageApi: RestUsageService,
        private optionsHelperService: OptionsHelperService,
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
        let parentInner: Element;
        let parent: Element;
        try {
            domCollections = document.evaluate(
                '//*[@id="edusharing_rendering_metadata"]//collections',
                document,
                null,
                XPathResult.FIRST_ORDERED_NODE_TYPE,
                null,
            ).singleNodeValue as Element;
            parentInner = domCollections.parentElement;
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

        function removeCollectionsContainer() {
            // if its the only widget inside the group
            if (parentInner.children.length === 2) {
                parent.remove();
                return;
            } else {
                const children = [...parentInner.children];
                let index = children.findIndex((child) => child === domCollections);
                if (index > 0) {
                    // remove caption
                    children[index - 1].remove();
                    children[index].remove();
                }
                return;
            }
        }

        this.getCollectionsContainingNode(node).subscribe(
            (collections) => {
                // @TODO: This does currently ignore the "hideIfEmpty" flag of the mds template
                if (collections.length === 0) {
                    removeCollectionsContainer();
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
                removeCollectionsContainer();
            },
        );
    }
    injectNodeRelationsWidget(node: Node) {
        let domRelations;
        try {
            domRelations = document.evaluate(
                '//*[@id="edusharing_rendering_metadata"]//nodeRelations',
                document,
                null,
                XPathResult.FIRST_ORDERED_NODE_TYPE,
                null,
            ).singleNodeValue as Element;
        } catch (e) {}
        if (domRelations) {
            domRelations = replaceElementWithDiv(domRelations);
            const component = UIHelper.injectAngularComponent(
                this.componentFactoryResolver,
                this.viewContainerRef,
                MdsNodeRelationsWidgetComponent,
                domRelations,
                {
                    node,
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

    /**
     * find injected link based actions with attribute es-action
     * (they're generated by the backend)
     * and replace them with internal function/action calls
     */
    applyActionButtons(node: Node) {
        let result: XPathResult;
        try {
            result = document.evaluate(
                '//*[@id="edusharing_rendering_metadata"]//a[@data-es-action]',
                document,
                null,
                XPathResult.UNORDERED_NODE_ITERATOR_TYPE,
                null,
            );
        } catch (e) {}
        if (result) {
            let action: HTMLElement;
            while ((action = result.iterateNext() as HTMLElement)) {
                const actionName = action.getAttribute('data-es-action');
                action.onclick = (event) => {
                    event.preventDefault();
                    this.optionsHelperService.setData({
                        scope: Scope.Render,
                        activeObjects: [node],
                    });
                    const option = this.optionsHelperService
                        .getAvailableOptions(Target.List, [node])
                        .filter((o) => o.name === actionName);
                    if (option.length === 1 && option[0].isEnabled) {
                        option[0].callback(node);
                    } else {
                        console.warn('No action was available for: ' + actionName);
                    }
                };
            }
        }
    }

    doAll(node: Node) {
        this.injectModuleInCollections(node);
        this.injectNodeRelationsWidget(node);
        this.injectModuleComments(node);
        this.applyActionButtons(node);
    }
}
