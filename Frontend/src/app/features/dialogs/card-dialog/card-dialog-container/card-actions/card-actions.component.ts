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
        // tslint:disable-next-line:no-bitwise
        this.buttonsRight = buttons?.filter((b) => (b.type & DialogButton.TYPE_SECONDARY) === 0);
        // tslint:disable-next-line:no-bitwise
        this.buttonsLeft = buttons?.filter(
            (b) => (b.type & DialogButton.TYPE_SECONDARY) === DialogButton.TYPE_SECONDARY,
        );
    }

    buttonsRight: DialogButton[];
    buttonsLeft: DialogButton[];
}
