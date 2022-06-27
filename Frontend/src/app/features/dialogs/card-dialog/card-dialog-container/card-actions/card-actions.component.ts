import { Component, Input, TemplateRef } from '@angular/core';
import { DialogButton } from '../../../../../core-module/ui/dialog-button';

@Component({
    selector: 'es-card-actions',
    templateUrl: './card-actions.component.html',
    styleUrls: ['./card-actions.component.scss'],
})
export class CardActionsComponent {
    @Input() additionalContent: TemplateRef<any>;

    @Input() set buttons(buttons: DialogButton[]) {
        this.buttonsRight = buttons?.filter((button) => button.position === 'standard');
        this.buttonsLeft = buttons?.filter((button) => button.position === 'opposite');
    }

    buttonsRight: DialogButton[];
    buttonsLeft: DialogButton[];
}
