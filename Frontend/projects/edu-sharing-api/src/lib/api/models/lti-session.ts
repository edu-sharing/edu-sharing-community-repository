export interface LtiSession {
    acceptMultiple: boolean;
    deeplinkReturnUrl: string;
    acceptTypes: string[];
    acceptPresentationDocumentTargets: string[];
    canConfirm: boolean;
    title: string;
    text: string;
}
