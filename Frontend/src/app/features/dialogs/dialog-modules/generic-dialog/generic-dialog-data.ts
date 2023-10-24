import { TemplateRef } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { ButtonConfig } from '../../../../core-module/ui/dialog-button';
import { CardDialogConfig } from '../../card-dialog/card-dialog-config';

export class GenericDialogData<R extends string> {
    /** Message text to show in the dialog body. Will be translated. */
    messageText?: string;
    /** Translation parameters for the given message text. */
    messageParameters?: { [key: string]: string };
    /** Custom template to use as dialog content. */
    contentTemplate?: TemplateRef<unknown>;
    /**
     * Buttons to include in the bottom bar of the dialog.
     *
     * Each button closes the dialog when clicked and passes its label to the `after_closed`
     * observable.
     */
    buttons?: GenericDialogButton<R>[];
}

export class GenericDialogConfig<R extends string> extends GenericDialogData<R> {
    title: CardDialogConfig['title'];
    nodes?: Node[];
    closable?: CardDialogConfig['closable'] = new CardDialogConfig().closable;
    minWidth?: CardDialogConfig['minWidth'];
    maxWidth?: CardDialogConfig['maxWidth'] = 750;
    customHeaderBarContent?: TemplateRef<HTMLElement>;
}

interface GenericDialogButton<R extends string> {
    label: R;
    config: ButtonConfig;
}

export const DELETE_OR_CANCEL: GenericDialogButton<'YES_DELETE' | 'CANCEL'>[] = [
    { label: 'CANCEL', config: { color: 'standard' } },
    { label: 'YES_DELETE', config: { color: 'danger' } },
];

export const YES_OR_NO: GenericDialogButton<'YES' | 'NO'>[] = [
    { label: 'NO', config: { color: 'standard' } },
    { label: 'YES', config: { color: 'primary' } },
];

export const DISCARD_OR_BACK: GenericDialogButton<'DISCARD' | 'BACK'>[] = [
    { label: 'BACK', config: { color: 'standard' } },
    { label: 'DISCARD', config: { color: 'primary' } },
];

export const REPLACE_OR_BACK: GenericDialogButton<'REPLACE' | 'BACK'>[] = [
    { label: 'BACK', config: { color: 'standard' } },
    { label: 'REPLACE', config: { color: 'primary' } },
];

export const SAVE_OR_CANCEL: GenericDialogButton<'SAVE' | 'CANCEL'>[] = [
    { label: 'CANCEL', config: { color: 'standard' } },
    { label: 'SAVE', config: { color: 'primary' } },
];
