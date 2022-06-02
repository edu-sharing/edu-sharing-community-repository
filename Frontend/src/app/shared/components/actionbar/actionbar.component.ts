import { trigger } from '@angular/animations';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { UIService } from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import { KeyCombination, OptionItem } from '../../../core-ui-module/option-item';
import { UIHelper } from '../../../core-ui-module/ui-helper';

@Component({
    selector: 'es-actionbar',
    templateUrl: 'actionbar.component.html',
    styleUrls: ['actionbar.component.scss'],
    animations: [trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
})
/**
 * The action bar provides several icons, usually at the top right, with actions for a current context
 */
export class ActionbarComponent implements OnChanges {
    /**
     * The amount of options which are not hidden inside an overflow menu
     * (default: depending on mobile (1) or not (2))
     * Also use numberOfAlwaysVisibleOptionsMobile to control the amount of mobile options visible
     */
    @Input() numberOfAlwaysVisibleOptions = 2;
    @Input() numberOfAlwaysVisibleOptionsMobile = 1;
    /**
     * Appearance show actionbar as button or circles (collection)
     * Values 'button' or 'round'
     * Appearance default = button;
     */
    @Input() appearance = 'button';
    /**
     * dropdownPosition is for position of dropdown (default = left)
     * Values 'left' or 'right'
     */
    @Input() dropdownPosition: 'left' | 'right' = 'left';

    /**
     * backgroundType for color matching, either bright, dark or primary
     */
    @Input() backgroundType: 'bright' | 'dark' | 'primary' = 'bright';
    /**
     * Style, currently default or 'flat' if all always visible icons should get a flat look
     */
    @Input() style: 'default' | 'flat' = 'default';
    /**
     * Highlight one or more of the always-visible buttons as primary action.
     *
     * - `first`, `last`: The first / last of `optionsAlways` by order.
     * - `manual`: Highlight all options that set `isPrimary = true`.
     */
    @Input() highlight: 'first' | 'last' | 'manual' = 'first';
    /**
     * Should disabled ("greyed out") options be shown or hidden?
     */
    @Input() showDisabled = true;
    /**
     * Set the options, see @OptionItem
     */
    @Input() set options(options: OptionItem[]) {
        this.optionsIn = options;
        this.prepareOptions(options);
    }

    optionsIn: OptionItem[] = [];
    optionsAlways: OptionItem[] = [];
    optionsMenu: OptionItem[] = [];
    optionsToggle: OptionItem[] = [];

    constructor(private ui: UIService, private translate: TranslateService) {}

    private prepareOptions(options: OptionItem[]) {
        options = UIHelper.filterValidOptions(this.ui, Helper.deepCopyArray(options));
        if (options == null) {
            this.optionsAlways = [];
            this.optionsMenu = [];
            return;
        }
        this.optionsToggle = UIHelper.filterToggleOptions(options, true);
        this.optionsAlways = this.getActionOptions(
            UIHelper.filterToggleOptions(options, false),
        ).slice(0, this.getNumberOptions());
        if (!this.optionsAlways.length) {
            this.optionsAlways = UIHelper.filterToggleOptions(options, false).slice(
                0,
                this.getNumberOptions(),
            );
        }
        this.optionsMenu = this.hideActionOptions(
            UIHelper.filterToggleOptions(options, false),
            this.optionsAlways,
        );
        // may causes weird looking
        /*if(this.optionsMenu.length<2) {
      this.optionsAlways = this.optionsAlways.concat(this.optionsMenu);
      this.optionsMenu = [];
    }*/
    }

    public getNumberOptions() {
        if (window.innerWidth < UIConstants.MOBILE_WIDTH) {
            return this.numberOfAlwaysVisibleOptionsMobile;
        }
        return this.numberOfAlwaysVisibleOptions;
    }

    click(option: OptionItem) {
        if (!option.isEnabled) {
            if (option.disabledCallback) {
                option.disabledCallback();
            }
            return;
        }
        option.callback();
    }

    private getActionOptions(options: OptionItem[]) {
        const result: OptionItem[] = [];
        for (const option of options) {
            if (option.showAsAction) result.push(option);
        }
        return result;
    }

    private hideActionOptions(options: OptionItem[], optionsAlways: OptionItem[]) {
        const result: OptionItem[] = [];
        for (const option of options) {
            if (optionsAlways.indexOf(option) === -1) result.push(option);
        }
        return result;
    }

    /**
     * Invalidate / refreshes all options based on their current callbacks
     */
    public invalidate() {
        this.prepareOptions(this.optionsIn);
    }

    private filterDisabled(options: OptionItem[]) {
        if (options == null) return null;
        const filtered = [];
        for (const option of options) {
            if (option.isEnabled || this.showDisabled) filtered.push(option);
        }
        return filtered;
    }

    canShowDropdown() {
        if (!this.optionsMenu.length) {
            return false;
        }
        return this.optionsMenu.filter((o) => o.isEnabled).length > 0;
    }

    shouldHighlight(optionIndex: number, option: OptionItem): boolean {
        switch (this.highlight) {
            case 'first':
                return optionIndex === 0;
            case 'last':
                return optionIndex === this.optionsAlways.length - 1;
            case 'manual':
                return option.isPrimary;
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.invalidate();
    }
}
