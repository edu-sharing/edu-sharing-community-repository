export abstract class Toast {
    abstract toast(message: string, translationParameters?: any): void;

    abstract error(errorObject: any, message?: string): void;
}
