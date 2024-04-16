import { InjectionToken, TemplateRef } from '@angular/core';
import { DialogButton } from '../../../util/dialog-button';
import { JumpMark } from '../../../services/jump-marks.service';
import { CardAvatar } from './card-dialog-container/card-header/card-avatar';

export const CARD_DIALOG_DATA = new InjectionToken<any>('CardDialogData');

export class CardDialogConfig<D = unknown> {
    title?: string;
    subtitle?: string;
    avatar?: CardAvatar;
    buttons?: DialogButton[];
    customHeaderBarContent?: TemplateRef<unknown>;
    customBottomBarContent?: TemplateRef<unknown>;
    contentPadding?: number = 25;
    width?: number;
    minWidth?: number | string;
    maxWidth?: number | string;
    height?: number;
    minHeight?: number | string;
    maxHeight?: number | string;
    closable?: Closable = Closable.Casual;
    /**
     * Element that should get initial focus after the dialog is opened.
     *
     * Note that for 'first-tabbable', any element that is given the attribute `cdkFocusInitial`
     * will get priority.
     */
    autoFocus?: AutoFocusTarget | string | boolean = 'first-tabbable';
    jumpMarks?: JumpMark[];
    data?: D;
}
export enum CARD_DEFAULT_WIDTH {
    xsmall = 400,
    small = 500,
    normal = 600,
    mlarge = 650,
    large = 700,
    xlarge = 800,
    xxlarge = 900,
    xxxlarge = 1200,
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

export type AutoFocusTarget = 'dialog' | 'first-tabbable' | 'first-heading';
