/**
 * Creative Commons deed urls for a license key.
 *
 * Kept free of Angular and rxjs on purpose: the edu-sharing Frontend-Server copies
 * this file verbatim (Frontend-Server/scripts/sync-shared.mjs) so the server rendered
 * `<head>` of a render view and the app agree on the license link. Do not add
 * imports here.
 */

export const LICENSE_URLS: { [key: string]: string } = {
    CC_BY_ABOUT: 'https://creativecommons.org/licenses/list.{{language}}',
    CC_BY: 'https://creativecommons.org/licenses/by/{{version}}/{{locale}}deed.{{language}}',
    CC_BY_ND: 'https://creativecommons.org/licenses/by-nd/{{version}}/{{locale}}deed.{{language}}',
    CC_BY_SA: 'https://creativecommons.org/licenses/by-sa/{{version}}/{{locale}}deed.{{language}}',
    CC_BY_NC: 'https://creativecommons.org/licenses/by-nc/{{version}}/{{locale}}deed.{{language}}',
    CC_BY_NC_ND:
        'https://creativecommons.org/licenses/by-nc-nd/{{version}}/{{locale}}deed.{{language}}',
    CC_BY_NC_SA:
        'https://creativecommons.org/licenses/by-nc-sa/{{version}}/{{locale}}deed.{{language}}',
    CC_0: 'https://creativecommons.org/publicdomain/zero/1.0/legalcode.{{language}}',
    PDM: 'https://creativecommons.org/public-domain/pdm/',
};

/**
 * The license url for a license key, or null when the key has none (`CUSTOM`, the
 * `EDU_*` keys and anything unknown).
 *
 * @param licenseProperty `ccm:commonlicense_key`
 * @param licenseVersion `ccm:commonlicense_cc_version`
 * @param licenseLocale `ccm:commonlicense_cc_locale`
 * @param language base language for the deed, e.g. `de` - `en` when empty
 */
export function getLicenseUrl(
    licenseProperty: string,
    licenseVersion: string,
    licenseLocale: string,
    language: string,
): string | null {
    // 4.0 licenses are not ported per jurisdiction, so they carry no country segment
    const isV4 = licenseVersion === '4.0';
    const locale = isV4 || !licenseLocale ? '' : licenseLocale.toLowerCase() + '/';
    const url = LICENSE_URLS[licenseProperty];
    if (!url) {
        return null;
    }
    return url
        .replace('{{version}}', licenseVersion)
        .replace('{{locale}}', locale)
        .replace('{{language}}', language || 'en');
}
