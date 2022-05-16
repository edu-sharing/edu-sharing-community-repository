import { Input, Type, Directive } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import {
    Group,
    Node,
    Statistics,
    Person,
    ProposalNode,
} from '../../../../core-module/rest/data-object';
import { ListItem } from '../../../../core-module/ui/list-item';

@Directive()
export class ListWidget {
    @Input()
    get node(): Node | ProposalNode | Group | Person | Statistics {
        return this.nodeSubject.value;
    }
    set node(value: Node | ProposalNode | Group | Person | Statistics) {
        this.nodeSubject.next(value);
    }
    protected readonly nodeSubject = new BehaviorSubject<
        Node | ProposalNode | Group | Person | Statistics
    >(null); // node (or group/user)

    @Input() item: ListItem;

    /**
     * Provide a label for non-obvious fields that describes the field the given value belongs to.
     *
     * The label is included in a tooltip and made available for a11y technologies.
     *
     * Useful when the value is displayed without context.
     *
     * Other tooltips might be added even with this input set to `false`.
     */
    @Input()
    get provideLabel() {
        return this.provideLabelSubject.value;
    }
    set provideLabel(value) {
        this.provideLabelSubject.next(value);
    }
    protected readonly provideLabelSubject = new BehaviorSubject(false);

    constructor() {}
}

export type ListWidgetClass = {
    supportedItems: ListItem[];
} & Type<ListWidget>;
