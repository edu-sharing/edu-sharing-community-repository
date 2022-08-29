import { Injectable, TemplateRef } from '@angular/core';

@Injectable()
export class NodeEntriesTemplatesService {
    title: TemplateRef<any>;
    empty: TemplateRef<any>;
    actionArea: TemplateRef<any>;
    overlay: TemplateRef<any>;

    constructor() {}
}
