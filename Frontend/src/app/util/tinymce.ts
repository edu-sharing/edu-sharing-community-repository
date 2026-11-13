import { PlatformLocation } from '@angular/common';
import { Provider } from '@angular/core';
import { TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

/**
 * Path (relative to the base href) where TinyMCE is copied to by the angular.json assets config.
 */
const TINYMCE_ASSET_PATH = 'assets/tinymce/';

/**
 * Provides the locally bundled TinyMCE script for the `<editor>` component.
 * Without this provider, tinymce-angular falls back to loading TinyMCE from the Tiny Cloud CDN.
 */
export const TINYMCE_LOCAL_SCRIPT_PROVIDER: Provider = {
    provide: TINYMCE_SCRIPT_SRC,
    useFactory: (platformLocation: PlatformLocation) =>
        getTinyMceBaseUrl(platformLocation) + 'tinymce.min.js',
    deps: [PlatformLocation],
};

export function getTinyMceBaseUrl(platformLocation: PlatformLocation): string {
    return platformLocation.getBaseHrefFromDOM() + TINYMCE_ASSET_PATH;
}

/**
 * Maps the app language to the name of a TinyMCE language file (tinymce-i18n/langs8).
 * Only languages whose file name differs from the app language need to be listed here.
 */
const TINYMCE_LANGUAGE_MAPPING: Record<string, string> = {
    fr: 'fr-FR',
};

/**
 * Languages TinyMCE has no language file for: English is built in,
 * 'none' is the translation debug mode of the app.
 */
const TINYMCE_BUILT_IN_LANGUAGES = ['en', 'none'];

/**
 * @returns the TinyMCE language for the given app language or undefined to use the built-in English
 */
export function getTinyMceLanguage(language: string): string | undefined {
    if (!language || TINYMCE_BUILT_IN_LANGUAGES.includes(language)) {
        return undefined;
    }
    return TINYMCE_LANGUAGE_MAPPING[language] ?? language;
}
