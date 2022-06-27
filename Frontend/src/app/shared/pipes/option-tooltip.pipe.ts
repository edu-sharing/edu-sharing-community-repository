import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { KeyCombination, OptionItem } from '../../core-ui-module/option-item';

@Pipe({ name: 'optionTooltip' })
export class OptionTooltipPipe implements PipeTransform {
    constructor(private translate: TranslateService) {}

    transform(option: OptionItem, args: string[] = null): string {
        return (
            this.translate.instant(option.name) +
            (option.key ? ' (' + this.getKeyInfo(option) + ')' : '')
        );
    }

    getKeyInfo(option: OptionItem) {
        if (!option.key) {
            return '';
        }
        const modifiers = [];
        if (option.key === option.key.toUpperCase()) {
            modifiers.push(this.translate.instant('KEY_MODIFIER.SHIFT'));
        }
        if (
            option.keyCombination &&
            option.keyCombination.indexOf(KeyCombination.CtrlOrAppleCmd) !== -1
        ) {
            modifiers.push(this.translate.instant('KEY_MODIFIER.CTRL'));
        }
        return (
            (modifiers.length ? modifiers.join(' + ') + ' + ' : '') +
            option.key.replace('Key', '').toUpperCase()
        );
    }
}
