import {
    Component,
    HostListener,
    Input,
    QueryList,
    TemplateRef,
    ViewChildren,
} from '@angular/core';
import { MatButton } from '@angular/material/button';
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

    @ViewChildren(MatButton) buttonElements: QueryList<MatButton>;

    buttonsRight: DialogButton[];
    buttonsLeft: DialogButton[];

    @HostListener('keydown.ArrowLeft') onArrowLeft() {
        this.focusButton('left');
    }

    @HostListener('keydown.ArrowRight') onArrowRight() {
        this.focusButton('right');
    }

    /**
     * Focuses a button by relative position to the currently focused button.
     */
    private focusButton(direction: 'left' | 'right'): void {
        const buttons = this.buttonElements.filter((button) => !button.disabled);
        const currentlyFocusedIndex = buttons.findIndex(
            (button) => button._elementRef.nativeElement === document.activeElement,
        );
        if (currentlyFocusedIndex >= 0) {
            const delta = direction === 'left' ? -1 : 1;
            const toFocusIndex = (currentlyFocusedIndex + delta + buttons.length) % buttons.length;
            buttons[toFocusIndex].focus('keyboard');
        }
    }
}
