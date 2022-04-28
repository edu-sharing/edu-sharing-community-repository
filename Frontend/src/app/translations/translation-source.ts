/** The preferred source for language files. */
export enum TranslationSource {
    /** In dev mode, local files are used and in production, repository files are used (default). */
    Auto,
    /** Local files (assets/i18n) are used. */
    Local,
    /** Repository files are used. */
    Repository,
}
