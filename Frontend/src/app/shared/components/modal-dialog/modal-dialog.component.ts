import {
    Component,
    Input,
    Output,
    EventEmitter,
    OnInit,
    HostListener,
    ViewChild,
    ElementRef,
    QueryList,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { trigger } from '@angular/animations';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { DialogButton, Node } from '../../../core-module/core.module';

@Component({
    selector: 'es-modal-dialog',
    templateUrl: 'modal-dialog.component.html',
    styleUrls: ['modal-dialog.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
/**
 * An common edu-sharing modal dialog
 */
export class ModalDialogComponent {
    /**
     * priority, useful if the dialog seems not to be in the foreground
     * Values greater 0 will raise the z-index
     */
    @Input() priority = 0;
    /**
     * Wether or not this dialog can be closed using escape or the icon
     * @type {boolean}
     */
    @Input() isCancelable = false;
    /**
     * The title, will be translated automatically
     * The dialog will only be visible if the title is not null
     */
    @Input() title: string;
    /**
     * The message, will be translated automatically
     */
    @Input() message: string;
    /**
     * Additional dynamic content for your language string, use an object, e.g.
     * Language String: Hello {{ name }}
     * And use messageParameters={name:'World'}
     */
    @Input() messageParameters: any;

    @Input() node: Node | Node[];
    /**
     * Will be emitted when the users cancels the dialog
     * @type {EventEmitter}
     */
    @Output() onCancel = new EventEmitter();
    /** A list of buttons, see @DialogButton
     * Also use the DialogButton.getYesNo() and others if applicable!
     */
    @Input() public buttons: DialogButton[];

    @ViewChild('buttonElements') buttonElements: ElementRef;

    public click(btn: DialogButton) {
        btn.callback();
    }
    public cancel() {
        this.onCancel.emit();
    }
}
export enum ProgressType {
    Indeterminate = 'indeterminate',
}
