import { InjectionToken } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { DialogButton } from '../../../core-module/ui/dialog-button';
import { CardAvatar } from './card-dialog-container/card-header/card-avatar';

export const CARD_DIALOG_DATA = new InjectionToken<any>('CardDialogData');

export class CardDialogCardConfig {
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

export type ViewMode = 'mobile' | 'default';

export class CardDialogState {
    cardConfig$: Observable<CardDialogCardConfig>;
    viewMode$: Observable<ViewMode>;
    // TODO: consistent naming
    loading = new BehaviorSubject<boolean>(false);

    get cardConfig() {
        return this.cardConfigSubject.value;
    }

    private cardConfigSubject: BehaviorSubject<CardDialogCardConfig>;
    private viewModeSubject: BehaviorSubject<ViewMode>;

    constructor({ cardConfig }: { cardConfig: CardDialogCardConfig }) {
        this.cardConfigSubject = new BehaviorSubject(cardConfig);
        this.cardConfig$ = this.cardConfigSubject.asObservable();
        this.viewModeSubject = new BehaviorSubject<ViewMode>(null);
        this.viewMode$ = this.viewModeSubject.asObservable();
    }

    patchCardConfig(config: Partial<CardDialogCardConfig>): void {
        this.cardConfigSubject.next({ ...this.cardConfigSubject.value, ...config });
    }

    updateViewMode(mode: ViewMode): void {
        this.viewModeSubject.next(mode);
    }
}

export class CardDialogConfig<D> {
    data?: D;
    cardConfig?: CardDialogCardConfig;
}

export interface CardDialogContentComponent<D = {}, R = void> {
    data: D;
}
