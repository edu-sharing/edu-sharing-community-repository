import {
    Component,
    ElementRef,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import {MdsEditorWidgetBase, ValueType} from '../../../../features/mds/mds-editor/widgets/mds-editor-widget-base';
import {RestConstants} from '../../../../core-module/rest/rest-constants';
import {ViewInstanceService} from '../../../../features/mds/mds-editor/mds-editor-view/view-instance.service';
import {MdsEditorInstanceService, Widget} from '../../../../features/mds/mds-editor/mds-editor-instance.service';
import {MdsWidgetComponent} from '../../../../features/mds/mds-viewer/widget/mds-widget.component';
import {
    MdsEditorViewComponent
} from '../../../../features/mds/mds-editor/mds-editor-view/mds-editor-view.component';
import {DatePipe} from '@angular/common';
import {DateHelper} from '../../../../core-ui-module/DateHelper';
import {TranslateService} from '@ngx-translate/core';
import {FormatSizePipe} from '../../../../core-ui-module/pipes/file-size.pipe';
import {RestHelper} from '../../../../core-module/rest/rest-helper';
import {Node} from '../../../../core-module/rest/data-object';
import {
    RelationService,
    NodeService,
    RelationData
} from 'ngx-edu-sharing-api';
import {ListItem} from '../../../../core-module/ui/list-item';
import {forkJoin as observableForkJoin} from 'rxjs';

@Component({
    selector: 'es-mds-node-relations-widget',
    templateUrl: 'node-relations-widget.component.html',
    styleUrls: ['node-relations-widget.component.scss'],
})
export class MdsNodeRelationsWidgetComponent implements OnInit, OnChanges {
    loading = true;
    @Input() node: Node;
    relations: RelationData[];
    columns = [
        new ListItem('NODE', RestConstants.LOM_PROP_TITLE)
    ];
    versions: Node[];
    forkedChilds: Node[];

    constructor(
        private translate: TranslateService,
        private relationService: RelationService,
        private nodeService: NodeService,
    ) {
    }

    ngOnInit(): void {

    }

    ngOnChanges(changes?: SimpleChanges) {
        if (this.node) {
            observableForkJoin([
                this.nodeService.getForkedChilds(this.node.ref.id),
                this.nodeService.getPublishedCopies(this.node.ref.id),
                this.relationService.getRelations(this.node.ref.id),
            ]).subscribe(
                (result) => {
                    this.forkedChilds = result[0].nodes;
                    this.versions = result[1].nodes.reverse();
                    this.relations = result[2].relations;
                    this.loading = false;
                }
            )
        }
    }


    getRelationKeys() {
        return [...new Set(this.relations?.map(r => r.type))].sort();
    }

    getRelations(key: string) {
        return this.relations.filter(r => r.type === key);

    }

    isCurrentVersion(version: Node) {
        // collection refs always refer to the latest version
        if(this.node.aspects.includes(RestConstants.CCM_ASPECT_IO_REFERENCE)) {
            const publishedVersion = this.versions.find((v) => v.ref.id === this.node.properties[RestConstants.CCM_PROP_IO_ORIGINAL]?.[0]);
            if(publishedVersion) {
                return publishedVersion === version
            }
            return this.versions.indexOf(version) === 0;
        }
        return this.node.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0] === version.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0];
    }
}
