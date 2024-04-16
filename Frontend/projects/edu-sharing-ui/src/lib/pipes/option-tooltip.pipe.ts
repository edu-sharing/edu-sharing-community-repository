import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { OptionItem } from '../types/option-item';

@Pipe({ name: 'optionTooltip' })
export class OptionTooltipPipe implements PipeTransform {
    constructor(private translate: TranslateService) {}

    transform(option: OptionItem, args: string[] = null): string {
        return (
            this.translate.instant(option.name) +
            (option.keyboardShortcut ? ' (' + this.getKeyInfo(option) + ')' : '')
        );
    }

    getKeyInfo(option: OptionItem) {
        if (!option.keyboardShortcut) {
            return '';
        }
        const modifiers = [];
        if (option.keyboardShortcut.modifiers?.includes('Shift')) {
            modifiers.push(this.translate.instant('KEY_MODIFIER.SHIFT'));
        }
        if (option.keyboardShortcut.modifiers?.includes('Ctrl/Cmd')) {
            modifiers.push(this.translate.instant('KEY_MODIFIER.CTRL'));
        }
        return (
            (modifiers.length ? modifiers.join(' + ') + ' + ' : '') +
            option.keyboardShortcut.keyCode.replace('Key', '')
        );
    }
}
