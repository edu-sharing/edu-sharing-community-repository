import {Component, Input, OnChanges, SimpleChanges, ViewChild} from '@angular/core';
import {NodeEntriesService} from '../../../node-entries.service';
import {Node} from '../../../../core-module/rest/data-object';
import {NodeHelperService} from '../../../node-helper.service';
import {ColorHelper, PreferredColor} from '../../../../core-module/ui/color-helper';
import {
    ClickSource,
    InteractionType
} from '../../node-entries-wrapper/node-entries-wrapper.component';
import {OptionItem, Target} from '../../../option-item';
import {DropdownComponent} from '../../dropdown/dropdown.component';
import {MatMenuTrigger} from '@angular/material/menu';

@Component({
    selector: 'app-node-entries-card',
    templateUrl: 'node-entries-card.component.html',
    styleUrls: ['node-entries-card.component.scss']
})
export class NodeEntriesCardComponent<T extends Node> implements OnChanges {
    readonly InteractionType = InteractionType;
    readonly Target = Target;
    readonly ClickSource = ClickSource;
    @Input() dropdown: DropdownComponent;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;

    @Input() node: T;
    dropdownLeft: number;
    dropdownTop: number;
    constructor(
        public entriesService: NodeEntriesService<T>,
        public nodeHelper: NodeHelperService,
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {
    }

    getTextColor() {
        return ColorHelper.getPreferredColor(this.node.collection.color) === PreferredColor.Black ?
            '#000' : '#fff';
    }
    optionsOnCard() {
        const options = this.entriesService.options[Target.List];
        const always = options.filter((o) => o.showAlways);
        if (always.some((o) => o.showCallback(this.node))) {
           return always;
        }
        // we do NOT show any additional actions
        return [];
        // return options.filter((o) => o.showAsAction && o.showCallback(this.node)).slice(0, 3);
    }

    openContextmenu(event: MouseEvent) {
        event.stopPropagation();
        event.preventDefault();
        this.dropdownLeft = event.clientX;
        this.dropdownTop = event.clientY;
        if (!this.entriesService.selection.selected.includes(this.node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(this.node)
        }
        this.menuTrigger.openMenu();
    }

    getVisibleColumns() {
        return this.entriesService.columns.filter((c) => c.visible);
    }
}
