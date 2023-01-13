import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

/**
 * Provided by dialog cards that provide jump mark functionality.
 */
@Injectable()
export class JumpMarksService {
    jumpMarks: JumpMark[];

    private readonly _beforeScrollToJumpMarkSubject = new Subject<JumpMark>();
    private _doScrollToJumpMark: (jumpMark: JumpMark) => void;

    constructor() {}

    scrollToJumpMark(jumpMark: JumpMark): void {
        this._beforeScrollToJumpMarkSubject.next(jumpMark);
        this._doScrollToJumpMark(jumpMark);
    }

    beforeScrollToJumpMark(): Observable<JumpMark> {
        return this._beforeScrollToJumpMarkSubject.asObservable();
    }

    registerJumpMarkHandler(doScrollToJumpMark: (jumpMark: JumpMark) => void) {
        this._doScrollToJumpMark = doScrollToJumpMark;
    }
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
