import { Component, Input } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { OptionItem } from '../../types/option-item';
// TODO: Decide if providing focus highlights and ripples with this component is a good idea. When
// using `app-node-url` for cards, we might need highlights and ripples for the whole card while
// `app-node-url` should only wrap the title since links with lots of content confuse screen
// readers.

@Component({
    selector: 'es-option-button',
    template: `
        <button
            mat-icon-button
            color="primary"
            matTooltip="{{ option.name | translate }}"
            [class.display-none]="!(optionIsShown(option, node) | async)"
            [disabled]="!optionIsValid(option, node)"
            (click)="click(option, node)"
            attr.data-test="option-button-{{ option.name }}"
        >
            <i esIcon="{{ option.icon }}" [aria]="false"></i>
        </button>
    `,
})
export class OptionButtonComponent {
    @Input() option: OptionItem;
    @Input() node: Node;

    optionIsValid(optionItem: OptionItem, node: Node): boolean {
        if (optionItem.enabledCallback) {
            return optionItem.enabledCallback(node);
        }
        return optionItem.isEnabled;
    }

    async optionIsShown(optionItem: OptionItem, node: Node): Promise<boolean> {
        if (optionItem.showCallback) {
            return await optionItem.showCallback(node);
        }
        return true;
    }

    async click(option: OptionItem, node: Node) {
        if (await this.optionIsShown(option, node)) {
            option.callback(node);
        }
    }
}
