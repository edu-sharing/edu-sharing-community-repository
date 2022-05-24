import { InjectionToken } from '@angular/core';
import { DialogButton } from '../../../core-module/ui/dialog-button';
import { CardAvatar } from './card-dialog-container/card-header/card-avatar';

export const CARD_DIALOG_DATA = new InjectionToken<any>('CardDialogData');

export class CardDialogConfig<D = unknown> {
    title?: string;
    subtitle?: string;
    avatar?: CardAvatar;
    buttons?: DialogButton[];
    contentPadding?: number = 25;
    width?: number;
    minWidth?: number | string;
    maxWidth?: number | string = '95%';
    height?: number;
    minHeight?: number | string;
    maxHeight?: number | string = '95%';
    closable?: Closable = Closable.Casual;
    data?: D;
}

/**
 * Standard ways for the user to close the dialog, ordered by increasing resistance.
 */
export enum Closable {
    /**
     * The dialog can be closed by pressing Escape, clicking the 'X' button or clicking the
     * backdrop.
     */
    Casual,
    /**
     * The dialog can be closed by pressing Escape or clicking the 'X' button.
     */
    Standard,
    /**
     * The dialog can be closed by pressing Escape or clicking the 'X' button after confirming a
     * dialog.
     */
    Confirm,
    /**
     * The dialog cannot be closed via standard actions.
     */
    Disabled,
}

export interface CardDialogContentComponent<D = {}, R = void> {
    data: D;
}
