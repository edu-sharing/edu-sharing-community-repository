import { TemplateRef } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { ButtonConfig } from '../../../../core-module/ui/dialog-button';
import { CardDialogConfig } from '../../card-dialog/card-dialog-config';

export class GenericDialogData<R extends string> {
    /** Message to show in the dialog body. Will be translated. */
    message?: string;
    /** Translation parameters for the given message text. */
    messageParameters?: { [key: string]: string };
    /**
     * How the message should be displayed.
     *
     * - text: The message is displayed as is. New-line characters will be converted to <p> tags.
     * - preformatted: The message will be wrapped in a <pre> tag.
     * - html: The message will be rendered as HTML in a <div> tag.
     */
    messageMode?: 'text' | 'preformatted' | 'html' = 'text';
    /**
     * Custom template to use as dialog content.
     *
     * Can be used instead of or in addition to a message.
     */
    contentTemplate?: TemplateRef<unknown>;
    /**
     * A context to pass to the content template.
     */
    context?: unknown;
    /**
     * Buttons to include in the bottom bar of the dialog.
     *
     * Each button closes the dialog when clicked and passes its label to the `afterClosed`
     * observable.
     */
    buttons?: GenericDialogButton<R>[];
}

export class GenericDialogConfig<R extends string> extends GenericDialogData<R> {
    title: CardDialogConfig['title'];
    subtitle?: CardDialogConfig['subtitle'];
    avatar?: CardDialogConfig['avatar'];
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

export const CLOSE: GenericDialogButton<'CLOSE'>[] = [
    { label: 'CLOSE', config: { color: 'standard' } },
];

export const OK: GenericDialogButton<'OK'>[] = [{ label: 'OK', config: { color: 'primary' } }];

export const OK_OR_CANCEL: GenericDialogButton<'OK' | 'CANCEL'>[] = [
    { label: 'CANCEL', config: { color: 'standard' } },
    { label: 'OK', config: { color: 'primary' } },
];

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
