import { CardDialogConfig } from '../../card-dialog/card-dialog-config';

export class InputDialogData<P extends string = string> {
    /** Message to show in the dialog body. Will be translated. */
    message?: string;
    /** Translation parameters for the given message text. */
    messageParameters?: { [key in P]: string };
    /** Label of the input field. Will be translated. */
    label: string;
}

export class InputDialogConfig extends InputDialogData {
    title: CardDialogConfig['title'];
    subtitle?: CardDialogConfig['subtitle'];
    avatar?: CardDialogConfig['avatar'];
}

export type InputDialogResult = string | null;
