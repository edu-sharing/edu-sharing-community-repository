import { Injectable, TemplateRef } from '@angular/core';

@Injectable()
export class NodeEntriesTemplatesService {
    /**
     * custom heading/title above the current result set
     */
    title: TemplateRef<any>;

    /**
     * Additional top matter that is provided by the selected node-entries component.
     */
    entriesTopMatter: TemplateRef<unknown>;

    /**
     * custom container in case the data source is empty
     */
    empty: TemplateRef<any>;

    /**
     * custom container for the action area on each element
     * (small grid only)
     */
    actionArea: TemplateRef<any>;
    /**
     * custom container for showing an overlay across each element
     */
    overlay: TemplateRef<any>;

    constructor() {}
}
