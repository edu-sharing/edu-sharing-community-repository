import {
    Component,
    ElementRef,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import {MdsEditorWidgetBase, ValueType} from '../../../mds-editor/widgets/mds-editor-widget-base';
import {RestConstants} from '../../../../../core-module/rest/rest-constants';
import {ViewInstanceService} from '../../../mds-editor/mds-editor-view/view-instance.service';
import {MdsEditorInstanceService, Widget} from '../../../mds-editor/mds-editor-instance.service';
import {MdsWidgetComponent} from '../mds-widget.component';
import {
    MdsEditorViewComponent
} from '../../../mds-editor/mds-editor-view/mds-editor-view.component';
import {DatePipe} from '@angular/common';
import {DateHelper} from '../../../../../core-ui-module/DateHelper';
import {MdsWidgetType} from '../../../mds-editor/types';
import {TranslateService} from '@ngx-translate/core';
import {FormatSizePipe} from '../../../../../core-ui-module/pipes/file-size.pipe';
import {RestHelper} from '../../../../../core-module/rest/rest-helper';
import {Node} from '../../../../../core-module/rest/data-object';
import {
    RelationService,
    RelationData
} from 'ngx-edu-sharing-api';
import {ListItem} from '../../../../../core-module/ui/list-item';

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

    constructor(
        private translate: TranslateService,
        private relationService: RelationService,
    ) {
    }

    ngOnInit(): void {

    }

    ngOnChanges(changes?: SimpleChanges) {
        console.log(this.node);
        if (this.node) {
            this.relationService.getRelations(this.node.ref.id).subscribe(
                ({relations}) => {
                    this.relations = relations;
                    this.loading = false;
                }
            );
        }
    }


    getRelationKeys() {
        return [...new Set(this.relations?.map(r => r.type))].sort();
    }

    getRelations(key: string) {
        return this.relations.filter(r => r.type === key);

    }
}
