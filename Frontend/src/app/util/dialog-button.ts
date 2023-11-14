import type { OptionItem } from 'ngx-edu-sharing-ui';

export class ButtonConfig {
    color: 'standard' | 'primary' | 'danger' = 'standard';
    position?: 'standard' | 'opposite' = 'standard';
}

export class DialogButton {
    readonly color: ButtonConfig['color'];
    readonly position: ButtonConfig['position'];

    disabled = false;

    static getOkCancel(cancel: () => void, ok: () => void): DialogButton[] {
        return [
            new DialogButton('CANCEL', { color: 'standard' }, cancel),
            new DialogButton('OK', { color: 'primary' }, ok),
        ];
    }

    static getOk(ok: () => void): DialogButton[] {
        return DialogButton.getSingleButton('OK', ok);
    }

    static getCancel(cancel: () => void): DialogButton[] {
        return DialogButton.getSingleButton('CANCEL', cancel, 'standard');
    }

    static getSingleButton(
        label: string,
        ok: () => void,
        color: ButtonConfig['color'] = 'primary',
    ): DialogButton[] {
        return [new DialogButton(label, { color }, ok)];
    }

    static getYesNo(no: () => void, yes: () => void): DialogButton[] {
        return [
            new DialogButton('NO', { color: 'standard' }, no),
            new DialogButton('YES', { color: 'primary' }, yes),
        ];
    }

    static getNextCancel(cancel: () => void, next: () => void): DialogButton[] {
        return [
            new DialogButton('CANCEL', { color: 'standard' }, cancel),
            new DialogButton('NEXT', { color: 'primary' }, next),
        ];
    }

    static getSaveCancel(cancel: () => void, save: () => void): DialogButton[] {
        return [
            new DialogButton('CANCEL', { color: 'standard' }, cancel),
            new DialogButton('SAVE', { color: 'primary' }, save),
        ];
    }

    static fromOptionItem(options: OptionItem[]) {
        if (options == null) {
            return null;
        }
        return options.map((o) => {
            return new DialogButton(o.name, { color: 'primary' }, () => o.callback(null));
        });
    }

    /** Button config with `color: 'primary'` for compatibility. */
    static readonly TYPE_PRIMARY: ButtonConfig = { color: 'primary' };
    /** Button config with `color: 'standard'` for compatibility. */
    static readonly TYPE_CANCEL: ButtonConfig = { color: 'standard' };
    /** Button config with `color: 'danger'` for compatibility. */
    static readonly TYPE_DANGER: ButtonConfig = { color: 'danger' };

    /**
     * @param label the button name, which is used for the translation
     * @param config the button type
     * @param callback A function callback when this option is chosen.
     */
    constructor(public label: string, config: ButtonConfig, public callback: () => void) {
        config = { ...new ButtonConfig(), ...config };
        this.color = config.color;
        this.position = config.position;
    }
}
