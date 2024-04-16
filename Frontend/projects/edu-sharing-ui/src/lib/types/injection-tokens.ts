import { InjectionToken } from '@angular/core';

/**
 * A custom base path for the assets directory.
 *
 * This is useful when the application cannot be served from the base HREF set in index.html. This
 * is the case when it is embedded as a web component into another website.
 */
export const ASSETS_BASE_PATH = new InjectionToken<string>('ASSETS_BASE_PATH');
