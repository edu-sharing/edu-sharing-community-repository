import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { MdsEditorWidgetBase } from '../mds-editor-widget-base';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { Toast } from '../../../../../services/toast';
import { ListItem, ValueType } from 'ngx-edu-sharing-ui';
import { Node, NodeService } from 'ngx-edu-sharing-api';

/**
 * show a given node as a filter value
 * note: this widget does only allow removing of the filter, but no value selection!
 */
@Component({
    selector: 'es-mds-editor-widget-nodefilter',
    templateUrl: './mds-editor-widget-nodefilter.component.html',
    styleUrls: ['./mds-editor-widget-nodefilter.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetNodefilterComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    hidden$ = new BehaviorSubject(true);
    node$ = new BehaviorSubject<Node>(null);
    columns = [new ListItem('NODE', RestConstants.CM_PROP_TITLE)];
    constructor(
        toast: Toast,
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private nodeService: NodeService,
    ) {
        super(toast, mdsEditorInstance, translate);
    }
    async ngOnInit(): Promise<void> {
        const values = (await this.widget.getInitalValuesAsync())?.jointValues;
        await this.setValues(values);
    }

    private async setValues(values: string[]) {
        this.hidden$.next(!values?.length && this.widget.definition.hideIfEmpty);
        if (values?.length) {
            this.node$.next(await firstValueFrom(this.nodeService.getNode(values[0])));
        }
    }

    protected clearFilter() {
        this.widget.setValue(null, true);
        this.setValues(null);
    }
}
