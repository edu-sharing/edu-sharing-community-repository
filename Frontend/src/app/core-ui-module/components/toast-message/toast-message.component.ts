import {
    Component,
    Input,
    Output,
    EventEmitter,
    OnInit,
    ViewChild,
    ElementRef,
    HostListener,
    Inject,
} from '@angular/core';
import { MAT_SNACK_BAR_DATA, MatSnackBar } from '@angular/material/snack-bar';
import { Toast, ToastDuration, ToastMessage } from '../../toast';

@Component({
    selector: 'es-toast-message',
    templateUrl: 'toast-message.component.html',
    styleUrls: ['toast-message.component.scss'],
})
/**
 * A basic link that should be used whenever a button is not the best solution but rather a link is preferable
 * Will handle keyup.enter automatically for the click binding
 */
export class ToastMessageComponent {
    readonly TOAST_DURATION = ToastDuration;
    @Output() click = new EventEmitter();
    constructor(
        @Inject(MAT_SNACK_BAR_DATA) public data: ToastMessage,
        public snackBar: MatSnackBar,
        public toast: Toast,
    ) {}
}
