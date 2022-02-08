import { InjectionToken } from '@angular/core';

export interface SkipNavConfig {
    scrollOffset?: number | (() => number);
}

export const SKIP_NAV_CONFIG = new InjectionToken<SkipNavConfig>('skip-nav.config');