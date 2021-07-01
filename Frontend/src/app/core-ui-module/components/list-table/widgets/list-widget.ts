import { Input, Type, Directive } from '@angular/core';
import {
    Group,
    Node,
    Statistics,
    Person,
    ProposalNode
} from '../../../../core-module/rest/data-object';
import { ListItem } from '../../../../core-module/ui/list-item';

@Directive()
export class ListWidget {
    @Input() node: Node | ProposalNode | Group | Person | Statistics; // node (or group/user)
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
