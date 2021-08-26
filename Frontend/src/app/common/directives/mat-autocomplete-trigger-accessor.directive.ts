// From https://github.com/angular/components/issues/18030#issuecomment-568603637

import { Directive, Host, Input, OnInit, Optional, Self } from '@angular/core';
import {
    AUTOCOMPLETE_OPTION_HEIGHT,
    AUTOCOMPLETE_PANEL_HEIGHT,
    MatAutocompleteTrigger,
} from '@angular/material/autocomplete';
import { _countGroupLabelsBeforeOption, _getOptionScrollPosition } from '@angular/material/core';

@Directive({
    selector: '[appMatAutocompleteTriggerAccessor]',
})
export class MatAutocompleteTriggerAccessorDirective implements OnInit {
    @Input() optionHeight: number = AUTOCOMPLETE_OPTION_HEIGHT;
    @Input() panelHeight: number = AUTOCOMPLETE_PANEL_HEIGHT;

    constructor(@Host() @Self() @Optional() public _refTrigger: MatAutocompleteTrigger) {}

    public ngOnInit() {
        if (this._refTrigger === undefined || this._refTrigger === null) {
            return;
        }
        (this._refTrigger as any)._scrollToOption = this._scrollToOption.bind(
            this._refTrigger,
            this.optionHeight,
            this.panelHeight,
        );
    }

    private _scrollToOption(
        this: MatAutocompleteTrigger,
        optionHeight: number,
        panelHeight: number,
    ): void {
        const index = this.autocomplete._keyManager.activeItemIndex || 0;
        const labelCount = _countGroupLabelsBeforeOption(
            index,
            this.autocomplete.options,
            this.autocomplete.optionGroups,
        );
        if (index === 0 && labelCount === 1) {
            this.autocomplete._setScrollTop(0);
        } else {
            const newScrollPosition = _getOptionScrollPosition(
                index + labelCount,
                optionHeight,
                this.autocomplete._getScrollTop(),
                panelHeight,
            );
            this.autocomplete._setScrollTop(newScrollPosition);
        }
    }
}
