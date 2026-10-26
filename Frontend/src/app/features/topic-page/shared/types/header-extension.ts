import { EventEmitter, InputSignal } from '@angular/core';

/**
 * Contract of a component registered via `TopicPageGlobalService.registerCustomHeaderExtension`,
 * e.g. to show and manage the editorial members of a topic.
 */
export interface HeaderExtensionInterface {
    // inputs
    editorialMemberNodeIds: string[];
    editMode: InputSignal<boolean>;

    // outputs
    editorialMemberNodeIdsChanged: EventEmitter<string[]>;
}
