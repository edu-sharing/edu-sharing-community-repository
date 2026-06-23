import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * Branding configuration passed to the title override callback.
 */
export interface TitleConfig {
    branding: boolean;
    siteTitle: string;
}

/**
 * Callback to override the document title computed by the `esTitle` directive.
 *
 * Receives the current page title (the h1 heading or the directive's title input) and the resolved
 * branding configuration. Return the full document title to use, or `null`/`undefined` to fall back
 * to the default behavior.
 */
export type TitleOverrideCallback = (
    pageTitle: string,
    config: TitleConfig,
) => string | null | undefined;

/**
 * Service to configure global page title behaviour, i.e. a custom title rendering callback.
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalTitleService {
    /**
     * register a callback to override the document title set by the `esTitle` directive
     */
    readonly titleOverrideCallback = new BehaviorSubject<TitleOverrideCallback | null>(null);
}
