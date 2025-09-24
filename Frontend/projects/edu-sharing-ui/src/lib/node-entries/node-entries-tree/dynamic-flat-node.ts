import { signal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';

/**
 * Flat node with expandable, level and isLoading information
 */
export class DynamicFlatNode {
    constructor(
        public item: Partial<Node>,
        public level = 0,
        public expandable = false,
        public isLoading = signal(false),
    ) {}
}
