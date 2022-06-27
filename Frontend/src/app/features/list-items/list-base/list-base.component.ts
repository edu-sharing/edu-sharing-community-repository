import { Component, OnChanges, SimpleChanges } from '@angular/core';
import { AVAILABLE_LIST_WIDGETS, ListWidgetType } from '../available-widgets';
import { ListWidget } from '../list-widget';

@Component({
    selector: 'es-list-base',
    templateUrl: './list-base.component.html',
})
export class ListBaseComponent extends ListWidget implements OnChanges {
    widgetType: ListWidgetType;
    constructor() {
        super();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (this.node && this.item) {
            this.widgetType = Object.entries(AVAILABLE_LIST_WIDGETS).find(
                ([id, w]) =>
                    w.supportedItems.filter(
                        (i) =>
                            i.type === this.item.type &&
                            (i.name === this.item.name || i.name === '*'),
                    ).length > 0,
            )?.[0] as ListWidgetType;
        }
    }
}
