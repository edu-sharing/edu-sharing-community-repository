
export class DialogButton {
    public static getOkCancel(cancel : Function,ok : Function) : DialogButton[]{
        return [
            new DialogButton("CANCEL",DialogButton.TYPE_CANCEL,cancel),
            new DialogButton("OK",DialogButton.TYPE_PRIMARY,ok),
        ];
    }
    public static getOk(ok : Function) : DialogButton[]{
        return DialogButton.getSingleButton("OK",ok);
    }
    public static getCancel(cancel : Function) : DialogButton[]{
        return DialogButton.getSingleButton("CANCEL",cancel,DialogButton.TYPE_CANCEL);
    }
    public static getSingleButton(label:string,ok : Function,type = DialogButton.TYPE_PRIMARY) : DialogButton[]{
        return [
            new DialogButton(label,type,ok),
        ];
    }
    public static getYesNo(no : Function,yes : Function) : DialogButton[]{
        return [
            new DialogButton("NO",DialogButton.TYPE_CANCEL,no),
            new DialogButton("YES",DialogButton.TYPE_PRIMARY,yes),
        ];
    }
    public static getNextCancel(cancel : Function,next : Function) : DialogButton[]{
        return [
            new DialogButton("CANCEL",DialogButton.TYPE_CANCEL,cancel),
            new DialogButton("NEXT",DialogButton.TYPE_PRIMARY,next),
        ];
    }
    public static TYPE_PRIMARY=1;
    public static TYPE_CANCEL=2;
    /**
     * @param name the button name, which is used for the translation
     * @param type the button type, use one of the constants
     * @param callback A function callback when this option is choosen.
     */
    constructor(public name: string,public type : number, public callback: Function) {
    }

}
