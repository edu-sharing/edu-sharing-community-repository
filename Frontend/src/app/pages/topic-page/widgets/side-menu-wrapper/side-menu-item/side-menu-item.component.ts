import { CommonModule } from '@angular/common';
import {
    Component,
    EventEmitter,
    input,
    Input,
    InputSignal,
    Output,
    ViewEncapsulation,
} from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
    selector: 'es-side-menu-item',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [CommonModule, MatIcon, MatTooltipModule],
    templateUrl: './side-menu-item.component.html',
    styleUrls: ['./side-menu-item.component.scss'],
})
export class SideMenuItemComponent {
    @Input() color?: string;
    disabled: InputSignal<boolean> = input<boolean>(false);
    @Input() icon?: string;
    @Input() position: string = 'right';
    @Input() selectedMenuItem: string = '';
    @Input() title: string;

    @Output() itemClicked: EventEmitter<string> = new EventEmitter<string>();

    constructor() {}

    /**
     * Emits that this menu item was clicked.
     */
    menuItemClicked(): void {
        this.itemClicked.emit(this.title);
    }
}
