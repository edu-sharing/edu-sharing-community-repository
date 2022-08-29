import { trigger } from '@angular/animations';
import { Component } from '@angular/core';
import { DialogButton } from '../../../core-module/core.module';
import { Node } from '../../../core-module/rest/data-object.js';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { Toast } from '../../../core-ui-module/toast';
import { CardType } from '../../../shared/components/card/card.component';
import { ProgressType } from '../../../shared/components/modal-dialog/modal-dialog.component';

export enum DialogType {
    /**
     * centered modal dialog
     */
    ModalCard,
    /**
     * info message bar at the bottom
     */
    Infobar,
}

@Component({
    selector: 'es-modal-dialog-toast',
    templateUrl: 'modal-dialog-toast.component.html',
    styleUrls: ['modal-dialog-toast.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class ModalDialogToastComponent {
    readonly DialogType = DialogType;
    buttons: DialogButton[];
    private onCancel: () => void;
    node: Node | Node[];

    constructor(public toast: Toast) {
        this.toast.onShowModalDialog((data: ModalDialogOptions) => {
            Promise.resolve().then(() => {
                this.title = data.title;
                this.message = data.message;
                this.input = data.input;
                this.toast.dialogInputValue = '';
                this.progressType = data.progressType;
                this.cardType = data.cardType;
                this.dialogType = data.dialogType || DialogType.ModalCard;
                this.node = data.node;
                this.messageParameters = data.messageParameters;
                this.messageType = data.messageType || ModalMessageType.Text;
                this.priority = data.priority || 10;
                this.isCancelable = data.isCancelable;
                this.buttons = data.buttons;
                this.onCancel = data.onCancel;
                this.visible = this.title != null;
            });
        });
    }

    public visible = false;

    isCancelable = true;
    /**
     * Name/Label of the input that should be displayed
     */
    input: string;
    /**
     * The title, will be translated automatically
     */
    title: string;
    /**
     * The priority (for z-index). defaults to 10
     */
    priority: number;
    /**
     * The message, will be translated automatically
     */
    message: string;
    /**
     * Additional dynamic content for your language string, use an object, e.g.
     * Language String: Hello {{ name }}
     * And use messageParameters={name:'World'}
     */
    messageParameters: any;

    messageType = ModalMessageType.Text;
    /**
     * type of the progress to display. Null if this is not an progress dialog
     */
    progressType: ProgressType;
    cardType: CardType;
    dialogType: DialogType;
    /* value stored in the input, if enabled */
    inputValue: string;

    cancel() {
        this.visible = false;
        if (this.onCancel != null) this.onCancel();
        this.reset();
    }
    private reset() {
        this.onCancel = null;
    }
}
export class ModalDialogOptions {
    title: string;
    message: string;
    buttons?: DialogButton[];
    input?: string;
    progressType?: ProgressType;
    cardType?: CardType;
    dialogType?: DialogType;
    messageParameters?: any;
    priority?: number;
    messageType? = ModalMessageType.Text;
    node?: Node | Node[];
    isCancelable? = true;
    onCancel?: () => void;
}
export enum ModalMessageType {
    Text = 'text',
    HTML = 'html',
}
