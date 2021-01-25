import { Input, Type } from '@angular/core';
import { Group, Node, Person } from '../../../../core-module/rest/data-object';
import { ListItem } from '../../../../core-module/ui/list-item';

export class ListWidget {
    @Input() node: Node | Group | Person; // node (or group/user)
    @Input() item: ListItem;
    /**
     * Whether to add a tooltip to non-obvious fields that describes the field the given value
     * belongs to.
     *
     * Useful when the value is displayed without context.
     *
     * Other tooltips might be added even with this input set to `false`.
     */
    @Input() showFieldTooltip = false;

    constructor() {}
}

export type ListWidgetClass = {
    supportedItems: ListItem[];
} & Type<ListWidget>;
