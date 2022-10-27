export interface LicenseAgreementDialogData {
    licenseHtml: string;
    version: string;
}

export type LicenseAgreementDialogResult = 'accepted' | 'declined';
