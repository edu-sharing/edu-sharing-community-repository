export interface XmlAppPropertiesDialogData {
    appName: string;
    appXml: string;
    properties: any;
}

/**
 * True, if changes have been saved to the backend.
 */
export type XmlAppPropertiesDialogResult = boolean | null;
