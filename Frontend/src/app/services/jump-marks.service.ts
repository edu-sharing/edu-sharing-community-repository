import { EventEmitter, Injectable } from '@angular/core';

/**
 * Provided by dialog cards that provide jump mark functionality.
 *
 * Hooks up a jump-marks provider to descendants of the jump marks handler.
 */
@Injectable()
export class JumpMarksService {
    readonly beforeScrollToJumpMark = new EventEmitter<JumpMark>();
    readonly triggerScrollToJumpMark = new EventEmitter<JumpMark | string>();
}

export class JumpMark {
    /**
     *
     * @param id the id (as in html)
     * @param label the pre-translated label
     * @param icon the icon
     */
    constructor(public id: string, public label: string, public icon: string) {}
}
