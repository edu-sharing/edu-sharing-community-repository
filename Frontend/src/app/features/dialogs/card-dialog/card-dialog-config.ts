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
    width?: number;
    minWidth?: number | string;
    maxWidth?: number | string = '95%';
    height?: number;
    minHeight?: number | string;
    maxHeight?: number | string = '95%';
    maximizeThresholdWidth?: number;
    maximizeThresholdHeight?: number;
}

export type ViewMode = 'mobile' | 'default';

export class CardDialogState {
    cardConfig$: Observable<CardDialogCardConfig>;
    viewMode$: Observable<ViewMode>;

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
