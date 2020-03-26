import { Component, Input, ViewChild } from '@angular/core';
import { MatMenu } from '@angular/material/menu';
import { UIService } from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { OptionItem } from '../../option-item';
import { UIHelper } from '../../ui-helper';

/**
 * The dropdown is one base component of the action bar (showing more actions),
 * but can also be used standalone.
 */
@Component({
    selector: 'dropdown',
    templateUrl: 'dropdown.component.html',
    styleUrls: ['dropdown.component.scss'],
})
export class DropdownComponent {
    @ViewChild('dropdown', { static: true }) menu: MatMenu;

    @Input() position: 'left' | 'right' = 'left';

    @Input() set options(options: OptionItem[]) {
        this._options = UIHelper.filterValidOptions(
            this.ui,
            Helper.deepCopyArray(options),
        );
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
}
