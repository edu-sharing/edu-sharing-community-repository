import { Component, Input, ViewChild } from '@angular/core';
import { MatMenu, MatMenuContent, MatMenuTrigger } from '@angular/material/menu';
import { OptionItem } from '../types/option-item';
import { Helper } from '../util/helper';
import { UIService } from '../services/ui.service';

/**
 * The dropdown is one base component of the action bar (showing more actions),
 * but can also be used standalone.
 */
@Component({
    selector: 'es-dropdown',
    templateUrl: 'dropdown.component.html',
    styleUrls: ['dropdown.component.scss'],
})
export class DropdownComponent {
    @ViewChild('dropdown', { static: true }) menu: MatMenu;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;

    @Input() position: 'left' | 'right' = 'left';

    @Input() set options(options: OptionItem[]) {
        this._options = this.ui.filterValidOptions(Helper.deepCopyArray(options));
    }

    /**
     * The object that should be returned via the option's callback.
     *
     * Can be null
     */
    @Input() callbackObject: any;

    /**
     * Should disabled ("greyed out") options be shown or hidden?
     */
    @Input() showDisabled = true;

    /**
     * An additional class to add to the `mat-menu` instance.
     *
     * This is needed to customize the menu styling since the menu contents are
     * taken out of the host container by angular.
     */
    @Input() menuClass: string;

    _options: OptionItem[];

    constructor(private ui: UIService) {}

    click(option: OptionItem) {
        if (!option.isEnabled) {
            return;
        }
        setTimeout(() => option.callback(this.callbackObject));
    }

    isNewGroup(i: number) {
        if (i > 0) {
            return this._options[i].group !== this._options[i - 1].group;
        }
        return false;
    }

    /** Whether there are any enabled options so we can open the menu. */
    canShowDropdown(): boolean {
        // We can only open the dropdown menu, when there is at least one enabled option. Even when
        // there are options with `showDisabled: true`, showing a menu with no selectable option
        // causes a11y issues.
        return this._options?.some((o) => o.isEnabled);
    }
}
