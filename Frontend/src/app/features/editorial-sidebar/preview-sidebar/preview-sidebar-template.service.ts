import { Injectable, TemplateRef } from '@angular/core';

@Injectable({
    providedIn: 'root',
})
export class PreviewSidebarTemplateService {
    /**
     * custom metadata area (defaults to an instance of es-mds-editor-wrapper)
     */
    metadata: TemplateRef<unknown>;

    /**
     * custom row for actions, stats and more (above the metadata)
     */
    actionsRow: TemplateRef<unknown>;
}
