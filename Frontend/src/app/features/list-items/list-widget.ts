import { Input, Type, Directive } from '@angular/core';
import { Person } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { ProposalNode, Group, Statistics, ListItem } from '../../core-module/core.module';
import { UniversalNode } from '../../common/definitions';

@Directive()
export class ListWidget {
    @Input()
    get node(): UniversalNode | ProposalNode | Group | Person | Statistics {
        return this.nodeSubject.value;
    }
    set node(value: UniversalNode | ProposalNode | Group | Person | Statistics) {
        this.nodeSubject.next(value);
    }
    protected readonly nodeSubject = new BehaviorSubject<
        UniversalNode | ProposalNode | Group | Person | Statistics
    >(null); // node (or group/user)

    @Input()
    get item(): ListItem {
        return this.itemSubject.value;
    }
    set item(value: ListItem) {
        this.itemSubject.next(value);
    }
    protected readonly itemSubject = new BehaviorSubject<ListItem>(null);

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
