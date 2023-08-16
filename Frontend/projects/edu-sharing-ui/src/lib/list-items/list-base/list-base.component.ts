import { Component, Input, OnChanges, SimpleChanges, TemplateRef } from '@angular/core';
import { AVAILABLE_LIST_WIDGETS, ListWidgetType } from '../available-widgets';
import { ListWidget } from '../list-widget';
import { NodeEntriesGlobalService } from '../../node-entries/node-entries-global.service';
import { Node } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-list-base',
    templateUrl: './list-base.component.html',
})
export class ListBaseComponent extends ListWidget implements OnChanges {
    /**
     * use text only widgets (for table)
     */
    @Input() forceText = false;
    widgetType: ListWidgetType;
    customTemplate: TemplateRef<unknown>;
    constructor(private nodeEntriesGlobalService: NodeEntriesGlobalService) {
        super();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (this.node && this.item) {
            this.customTemplate = this.nodeEntriesGlobalService.getCustomFieldTemplate(
                this.item,
                this.node as Node,
            );
            if (this.customTemplate) {
                this.widgetType = ListWidgetType.Custom;
            } else if (this.forceText) {
                this.widgetType = ListWidgetType.Text;
            } else {
                this.widgetType = Object.entries(AVAILABLE_LIST_WIDGETS).find(
                    ([id, w]) =>
                        w?.supportedItems?.filter(
                            (i) =>
                                i.type === this.item.type &&
                                (i.name === this.item.name || i.name === '*'),
                        ).length > 0,
                )?.[0] as ListWidgetType;
            }
        }
    }
}
