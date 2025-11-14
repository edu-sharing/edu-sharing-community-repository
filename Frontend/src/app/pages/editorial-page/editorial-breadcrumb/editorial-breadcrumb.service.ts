import { Injectable, signal } from '@angular/core';

@Injectable()
export class EditorialBreadcrumbService {
    /**
     * the current global mode (will be translated)
     */
    readonly mode = signal<string>(null);
    /**
     * any additional path elements, i.e. an element currently being interacted with
     */
    readonly path = signal<string[]>([]);
}
