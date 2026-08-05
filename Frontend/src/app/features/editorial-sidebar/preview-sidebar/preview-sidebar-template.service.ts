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

    /**
     * custom content area (defaults to an instance of es-render-wrapper-component)
     *
     * Context: `{ element, modal }` (the node being rendered and whether the content is displayed
     * in the fullscreen modal). Use it to attach custom directives to the render wrapper - e.g. to
     * inject additional elements into a rendering service module - without modifying the wrapper
     * or the rendering service library.
     */
    render: TemplateRef<unknown>;
}
