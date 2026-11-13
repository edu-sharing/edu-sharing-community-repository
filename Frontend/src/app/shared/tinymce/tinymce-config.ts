/**
 * Shared TinyMCE configuration for all editor instances (MDS widget, admin messages, editorial
 * assignments). TinyMCE is self-hosted from `assets/tinymce` (see `angular.json`).
 */

/**
 * App language → file name in `tinymce-i18n/langs8` (copied to `assets/tinymce/langs`).
 * English is built into TinyMCE and needs no language file.
 */
const TINYMCE_LANGUAGES: Record<string, string> = {
    de: 'de',
    en: 'en',
    // langs8 only ships `fr-FR.js`, there is no `fr.js`
    fr: 'fr-FR',
    it: 'it',
};

export function getTinyMceLanguage(language: string): string {
    return TINYMCE_LANGUAGES[language] ?? TINYMCE_LANGUAGES[language?.split('-')[0]] ?? 'en';
}

/**
 * Options every editor instance needs.
 * @param baseHref the app's base href, i.e. `PlatformLocation.getBaseHrefFromDOM()`
 * @param language the app language, e.g. `TranslateService.getDefaultLang()`
 */
export function getTinyMceBaseConfig(baseHref: string, language: string): Record<string, any> {
    return {
        // TinyMCE >= 8 disables the self-hosted editor without a license key; we use it under GPL
        license_key: 'gpl',
        base_url: (baseHref ?? '/') + 'assets/tinymce/',
        language: getTinyMceLanguage(language),
        branding: false,
    };
}

/** Skin options; TinyMCE cannot swap skins on a live instance, so re-create the editor on change. */
export function getTinyMceThemeConfig(isDarkMode: boolean): Record<string, any> {
    return {
        skin: isDarkMode ? 'oxide-dark' : 'oxide',
        content_css: isDarkMode ? 'dark' : 'default',
    };
}

/** Editor of the admin messages */
export const AdminMessagesEditorConfig: Record<string, any> = {
    // TinyMCE rewrites href/src relative to its document_base_url by
    // default; keep admin-entered message URLs exactly as typed. (breaks angular logic)
    convert_urls: false,
    height: 200,
    apiKey: '',
    menubar: false,
    statusbar: false,
    resize: true,
    //newline_behavior: 'linebreak',
    plugins: ['link', 'code'],
    toolbar:
        'bold italic underline | link | alignleft aligncenter alignright alignjustify | removeformat | code | undo redo',
};

/** Editors of the editorial assignments (manage / submit / submission feedback) */
export const AssignmentEditorConfig: Record<string, any> = {
    height: 200,
    suffix: '.min',
    menubar: false,
    statusbar: false,
    resize: true,
    plugins: ['lists'],
    default_link_target: '_blank',
    link_title: false,
    newline_behavior: 'invert',
    link_assume_external_targets: true,
    toolbar: 'bold | bullist numlist | undo redo',
};
