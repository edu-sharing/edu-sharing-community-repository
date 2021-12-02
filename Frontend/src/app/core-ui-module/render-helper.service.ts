import {TranslateService} from '@ngx-translate/core';
import {FormatSizePipe} from './pipes/file-size.pipe';
import {Observable, Observer} from 'rxjs';
import {Params, Router} from '@angular/router';
import {DefaultGroups, OptionGroup, OptionItem} from './option-item';
import {DateHelper} from './DateHelper';
import {UIConstants} from '../core-module/ui/ui-constants';
import {Helper} from '../core-module/rest/helper';
import {VCard} from '../core-module/ui/VCard';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {ConfigurationHelper} from '../core-module/rest/configuration-helper';
import {MessageType} from '../core-module/ui/message-type';
import {Toast} from './toast';
import {UIHelper} from './ui-helper';
import {ComponentFactoryResolver, Injectable, ViewContainerRef} from '@angular/core';
import {BridgeService} from '../core-bridge-module/bridge.service';
import {
    AuthorityProfile,
    CollectionReference,
    NodesRightMode,
    Node,
    Permission,
    User,
    WorkflowDefinition,
    Repository,
    ProposalNode
} from '../core-module/rest/data-object';
import {TemporaryStorageService} from '../core-module/rest/services/temporary-storage.service';
import {RestConstants} from '../core-module/rest/rest-constants';
import {ConfigurationService} from '../core-module/rest/services/configuration.service';
import {RestHelper} from '../core-module/rest/rest-helper';
import {RestConnectorService} from '../core-module/rest/services/rest-connector.service';
import {ListItem} from '../core-module/ui/list-item';
import {RestNetworkService} from '../core-module/rest/services/rest-network.service';
import {SpinnerSmallComponent} from './components/spinner-small/spinner-small.component';
import {AVAILABLE_LIST_WIDGETS} from './components/list-table/widgets/available-widgets';
import {NodePersonNamePipe} from './pipes/node-person-name.pipe';
import {SpinnerComponent} from './components/spinner/spinner.component';
import {ListTableComponent} from './components/list-table/list-table.component';
import {RestUsageService} from '../core-module/rest/services/rest-usage.service';
import {CommentsListComponent} from '../modules/management-dialogs/node-comments/comments-list/comments-list.component';
import { replaceElementWithDiv } from '../common/ui/mds-editor/util/replace-element-with-div';
import {MdsEditorEmbeddedComponent} from '../common/ui/mds-editor/mds-editor-embedded/mds-editor-embedded.component';
import {MdsEditorCoreComponent} from '../common/ui/mds-editor/mds-editor-core/mds-editor-core.component';
import {MdsEditorWrapperComponent} from '../common/ui/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import {EditorMode} from './mds-types';

@Injectable()
export class RenderHelperService {
    private viewContainerRef: ViewContainerRef;

    constructor(
        private translate: TranslateService,
        private componentFactoryResolver: ComponentFactoryResolver,
        private config: ConfigurationService,
        private rest: RestConnectorService,
        private bridge: BridgeService,
        private http: HttpClient,
        private connector: RestConnectorService,
        private usageApi: RestUsageService,
        private toast: Toast,
        private router: Router,
        private storage: TemporaryStorageService,
    ) {
    }
    setViewContainerRef(viewContainerRef: ViewContainerRef) {
        this.viewContainerRef = viewContainerRef;
    }
    private static isCollectionRef(node: Node) {
        return node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1;
    }
    injectModuleComments(node: Node) {
        const data= {
            node
        };
        UIHelper.injectAngularComponent(this.componentFactoryResolver,
            this.viewContainerRef,
            CommentsListComponent,
            document.getElementsByTagName('comments')[0],
            data
        );
    }
    injectModuleInCollections(node: Node) {
        let domContainer:Element;
        let domCollections:Element;
        try {
            domContainer = document.getElementsByClassName('node_collections_render')[0].parentElement;
            domCollections = document.getElementsByTagName('collections')[0];
        } catch(e) {
            return;
        }
        domCollections = replaceElementWithDiv(domCollections);
        UIHelper.injectAngularComponent(this.componentFactoryResolver,this.viewContainerRef,SpinnerComponent,domCollections);
        this.usageApi.getNodeUsagesCollection(
            RenderHelperService.isCollectionRef(node) ? node.properties[RestConstants.CCM_PROP_IO_ORIGINAL] : node.ref.id,node.ref.repo
        ).subscribe((usages)=> {
            usages = usages.filter((u) => u.collectionUsageType === 'ACTIVE');
            // @TODO: This does currently ignore the "hideIfEmpty" flag of the mds template
            if(usages.length === 0) {
                domContainer.parentElement.removeChild(domContainer);
                return;
            }
            const data= {
                nodes:usages.map((u)=>u.collection),
                columns:ListItem.getCollectionDefaults(),
                isClickable:true,
                clickRow:(event: {node: Node})=> {
                    UIHelper.goToCollection(this.router,event.node);
                },
                doubleClickRow:(event: Node)=> {
                    UIHelper.goToCollection(this.router,event);
                },
                viewType:ListTableComponent.VIEW_TYPE_GRID_SMALL,
            };
            UIHelper.injectAngularComponent(this.componentFactoryResolver,
                this.viewContainerRef,
                ListTableComponent,
                domCollections,
                data,
                { delay: 250 }
            );
        },(error)=> {
            domContainer.parentElement.removeChild(domContainer);
        });
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
            }
        );

    }
}
