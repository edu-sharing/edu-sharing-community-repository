import { Directive, Input, Type } from '@angular/core';
import { Group, Node, Person, ProposalNode, Statistics, Submission } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { BaseListItem, ListItem } from '../types/list-item';

@Directive()
export class ListWidget {
    @Input()
    get node(): Node | ProposalNode | Group | Person | Statistics | Submission {
        return this.nodeSubject.value;
    }
    set node(value: Node | ProposalNode | Group | Person | Statistics | Submission) {
        this.nodeSubject.next(value);
    }
    protected readonly nodeSubject = new BehaviorSubject<
        Node | ProposalNode | Group | Person | Statistics | Submission
    >(null); // node (or group/user)

    @Input()
    get item(): BaseListItem {
        return this.itemSubject.value;
    }
    set item(value: BaseListItem) {
        this.itemSubject.next(value);
    }
    protected readonly itemSubject = new BehaviorSubject<BaseListItem>(null);

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
