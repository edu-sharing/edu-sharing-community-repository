import { Component, OnInit } from '@angular/core';
import {MdsEditorInstanceService} from '../../mds-editor-instance.service';
import {RestConstants} from '../../../../../core-module/rest/rest-constants';
import {VCard} from '../../../../../core-module/ui/VCard';
import {UIService} from '../../../../../core-module/rest/services/ui.service';
import {Node, NodeWrapper} from '../../../../../core-module/rest/data-object';
import {RestIamService} from '../../../../../core-module/rest/services/rest-iam.service';
import {NativeWidgetComponent} from '../../mds-editor-view/mds-editor-view.component';
import {BehaviorSubject, Observable, Subscriber} from 'rxjs';
import {Helper} from '../../../../../core-module/rest/helper';
import {Values} from '../../types';
import {MainNavService} from '../../../../services/main-nav.service';
import {RestNodeService} from '../../../../../core-module/rest/services/rest-node.service';
import {RestHelper} from '../../../../../core-module/rest/rest-helper';
import {RestConnectorService} from '../../../../../core-module/rest/services/rest-connector.service';
import {RestUtilitiesService} from '../../../../../core-module/rest/services/rest-utilities.service';
import {Toast} from '../../../../../core-ui-module/toast';
import {CdkDragDrop, moveItemInArray} from '@angular/cdk/drag-drop';
import {NodeHelperService} from '../../../../../core-ui-module/node-helper.service';
import {distinctUntilChanged} from 'rxjs/operators';


interface Childobject {
    icon: string,
    name: string,
    link?: string;
    node?: Node,
    file?: File,
    properties?: Values
}
interface ChildobjectEdit {
    child: Childobject;
    properties: Values;
}
@Component({
    selector: 'app-mds-editor-widget-childobjects',
    templateUrl: './mds-editor-widget-childobjects.component.html',
    styleUrls: ['./mds-editor-widget-childobjects.component.scss'],
})
export class MdsEditorWidgetChildobjectsComponent implements OnInit, NativeWidgetComponent {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    hasChanges = new BehaviorSubject<boolean>(false);
    children: Childobject[] = [];
    add = false;
    _edit: ChildobjectEdit;
    _editLicense: ChildobjectEdit;
    childrenDelete: Node[] = [];
    constructor(
        private mdsEditorValues: MdsEditorInstanceService,
        private nodeApi: RestNodeService,
        private connector: RestConnectorService,
        private utilities: RestUtilitiesService,
        private nodeHelper: NodeHelperService,
        public toast: Toast,
    ) {}

    ngOnInit(): void {
        this.mdsEditorValues.nodes$.pipe(
            distinctUntilChanged((a ,b) => a?.[0]?.ref?.id === b?.[0]?.ref?.id)
        ).filter((n) => n != null).subscribe(async (nodes) => {
            if (nodes?.length) {
                this.children = (await this.nodeApi.getNodeChildobjects(nodes[0].ref.id).toPromise()).nodes.map((n) => {
                    return {
                        icon: n.iconURL,
                        name: RestHelper.getTitle(n),
                        node: n,
                        properties: n.properties,
                    }
                });
            }
        });
    }
    onChange(): void {
        this.hasChanges.next(true);
    }

    addFiles(files: FileList) {
        for (let i = 0; i < files.length; i++) {
            const file = files.item(i);
            const child: Childobject = {
                icon: RestHelper.guessMediatypeIconForFile(this.connector, file),
                name: file.name,
                file,
            };
            this.children.push(child);
        }
        this.add = false;
        this.onChange();
    }
    addLink(linkData: any) {
        const link = this.nodeHelper.addHttpIfRequired(linkData.link);
        this.add = false;
        const properties = RestHelper.createNameProperty(link);
        properties[RestConstants.CCM_PROP_IO_WWWURL] = [link];
        properties[RestConstants.LOM_PROP_TITLE] = [link];
        const process = () => {
            const data: Childobject = {
                icon: this.connector.getThemeMimeIconSvg('link.svg'),
                name: RestHelper.getTitleFromProperties(properties),
                link,
                properties,
            };
            this.children.push(data);
            this.onChange();
        };
        this.utilities.getWebsiteInformation(link).subscribe(
            (info) => {
                if (info.title)
                    properties[RestConstants.LOM_PROP_TITLE] = [info.title + ' - ' + info.page];
                process();
            },
            (error) => {
                console.warn(error);
                process();
            },
        );
    }
    setProperties(props: Values) {
        const edit = this._edit ?? this._editLicense;
        // keep any existing license data
        if(props) {
            if (!edit.child.properties) {
                edit.child.properties = edit.properties;
            }
            if (this._edit) {
                for (const key of Object.keys(props)) {
                    edit.child.properties[key] = props[key];
                }
            }
            edit.child.name = edit.child.properties[RestConstants.LOM_PROP_TITLE]?.[0]
                ? edit.child.properties[RestConstants.LOM_PROP_TITLE][0]
                : edit.child.properties[RestConstants.CM_NAME][0];
        }
        this._edit = null;
        this._editLicense = null;
        this.onChange();
    }

    private getProperties(child: Childobject) {
        let props: Values;
        if (child.properties) {
            props = child.properties;
        } else if (child.file) {
            props = RestHelper.createNameProperty(child.name);
        } else {
            console.error('Invalid object state for childobject', child);
            return null;
        }
        props[RestConstants.CCM_PROP_CHILDOBJECT_ORDER] = [this.children.indexOf(child) + ''];
        return props;
    }
    edit(child: Childobject) {
        this._edit = {
            child: child,
            properties: this.getProperties(child),
        };
    }
    editLicense(child: Childobject) {
        this._editLicense = {
            child: child,
            properties: this.getProperties(child),
        };
    }
    remove(child: Childobject) {
        this.children.splice(this.children.indexOf(child), 1);
        if(child.node) {
            this.childrenDelete.push(child.node);
        }
        this.onChange();
    }
    async onSaveNode(nodes: Node[]) {
        await Observable.forkJoin(
            this.children.map((child) =>
                new Observable<Node>((observer) => {
                if (child.file) {
                    this.nodeApi
                        .createNode(
                            nodes[0].ref.id,
                            RestConstants.CCM_TYPE_IO,
                            [RestConstants.CCM_ASPECT_IO_CHILDOBJECT],
                            this.getProperties(child),
                            true,
                            '',
                            RestConstants.CCM_ASSOC_CHILDIO,
                        )
                        .subscribe((data: NodeWrapper) => {
                            this.nodeApi
                                .uploadNodeContent(
                                    data.node.ref.id,
                                    child.file,
                                    RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                                )
                                .subscribe(
                                    () => {
                                        observer.complete();
                                    },
                                    (error) => {
                                        if (
                                            RestHelper.errorMatchesAny(
                                                error,
                                                RestConstants.CONTENT_QUOTA_EXCEPTION,
                                            )
                                        ) {
                                            this.nodeApi
                                                .deleteNode(data.node.ref.id, false)
                                                .subscribe(() => {
                                                    observer.complete();
                                                });
                                            this.toast.error(null, 'MDS.ADD_CHILD_OBJECT_QUOTA_REACHED', {
                                                name: child.name,
                                            });
                                        }
                                    },
                                );
                        });
                } else if (child.link) {
                    let properties: any = {};
                    properties[RestConstants.CCM_PROP_IO_WWWURL] = [child.link];
                    this.nodeApi
                        .createNode(
                            nodes[0].ref.id,
                            RestConstants.CCM_TYPE_IO,
                            [RestConstants.CCM_ASPECT_IO_CHILDOBJECT],
                            this.getProperties(child),
                            true,
                            RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                            RestConstants.CCM_ASSOC_CHILDIO,
                        )
                        .subscribe(() => {
                            observer.complete();
                        });
                } else {
                    this.nodeApi
                        .editNodeMetadata(child.node.ref.id, this.getProperties(child))
                        .subscribe(() => {
                            observer.complete();
                        });
                }
            }))).toPromise();
            await this.deleteChildren();
            return nodes;
    }
    drop(event: CdkDragDrop<string[]>) {
        moveItemInArray(this.children, event.previousIndex, event.currentIndex);
        this.onChange();
    }

    private async deleteChildren() {
        if(this.childrenDelete.length) {
            await Observable.forkJoin(this.childrenDelete.map((node) => this.nodeApi.deleteNode(node.ref.id, false))).toPromise();
        }
    }
}
