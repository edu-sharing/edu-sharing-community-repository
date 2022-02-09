import { InjectionToken } from '@angular/core';

export interface CardConfig {
    forceModalAlways?: boolean;
}

export const CARD_CONFIG = new InjectionToken<CardConfig>('card.config');