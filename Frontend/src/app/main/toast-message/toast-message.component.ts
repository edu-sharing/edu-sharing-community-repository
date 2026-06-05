import { Component, inject } from '@angular/core';
import { MAT_SNACK_BAR_DATA } from '@angular/material/snack-bar';
import { ToastMessage } from '../../services/toast';

@Component({
    selector: 'es-toast-message',
    templateUrl: 'toast-message.component.html',
    styleUrls: ['toast-message.component.scss'],
    standalone: false,
})
/**
 * A basic link that should be used whenever a button is not the best solution but rather a link is preferable
 * Will handle keyup.enter automatically for the click binding
 */
export class ToastMessageComponent {
    data = inject<ToastMessage>(MAT_SNACK_BAR_DATA);
}
