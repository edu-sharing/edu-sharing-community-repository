import {Component, Input, OnInit, SimpleChanges} from '@angular/core';
import {ListWidget} from '../list-widget';
import {ListItem} from '../../../../../core-module/ui/list-item';
import {RestConstants} from '../../../../../core-module/rest/rest-constants';
import {AVAILABLE_LIST_WIDGETS, ListWidgetType} from '../available-widgets';

@Component({
    selector: 'app-list-base',
    templateUrl: './list-base.component.html',
})
export class ListBaseComponent extends ListWidget {
    widgetType: ListWidgetType;


    constructor() {
        super();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if(this.node && this.item){
            this.widgetType = Object.entries(AVAILABLE_LIST_WIDGETS).find(([id, w]) => w.supportedItems.filter(
                (i) => i.type === this.item.type && (i.name === this.item.name || i.name === '*')).length > 0
            )?.[0] as ListWidgetType;
            console.log('type: ', this.widgetType);
        }
    }
}
