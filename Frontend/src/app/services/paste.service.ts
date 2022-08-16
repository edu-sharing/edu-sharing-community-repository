import { Injectable, NgZone } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { filter, switchMap, map } from 'rxjs/operators';
import { runInZone } from '../util/rxjs-operators/run-in-zone';

/**
 * The minimum time in milliseconds for a paste event to follow an aux click event.
 *
 * If the paste event follows within this time, we will discard it.
 *
 * We do this because on Linux, middle clicks cause paste events. However, middle clicks are also
 * used to open links in new tabs. For any custom paste handlers it does not really make sense to
 * handle paste events caused by middle clicks.
 *
 * Note that the `auxclick` event is not supported on Safari, but since MacOS does not paste on
 * middle clicks, we don't need it there.
 */
const AUX_CLICK_PASTE_MIN_DIFF_MS = 100;

@Injectable({
    providedIn: 'root',
})
export class PasteService {
    private lastAuxClickTime = 0;
    private pasteSubjectOutsideZone = new Subject<ClipboardEvent>();

    constructor(private ngZone: NgZone) {
        ngZone.runOutsideAngular(() => {
            document.addEventListener('auxclick', () => this.onAuxClick());
            document.addEventListener('paste', (event) => this.onPaste(event));
        });
    }

    /**
     * Observes paste events that are triggered outside of input fields and contain HTTP or HTTPS
     * urls.
     */
    observeUrlPasteOnPage(): Observable<string> {
        return this.pasteSubjectOutsideZone.pipe(
            filter((event) => !targetIsInputOrTextarea(event)),
            switchMap(getPlainTextString),
            filter(notNull),
            filter(isUrl),
            runInZone(this.ngZone),
        );
    }

    /**
     * Observes paste events that are triggered outside of input fields and contain no plaintext
     * representation.
     */
    observeNonTextPageOnPage(): Observable<void> {
        return this.pasteSubjectOutsideZone.pipe(
            filter((event) => !targetIsInputOrTextarea(event)),
            filter((event) => !hasPlainTextString(event)),
            map(() => void 0),
            runInZone(this.ngZone),
        );
    }

    private onAuxClick() {
        this.lastAuxClickTime = new Date().getTime();
    }

    private onPaste(event: ClipboardEvent) {
        const now = new Date().getTime();
        if (now - this.lastAuxClickTime >= AUX_CLICK_PASTE_MIN_DIFF_MS) {
            this.pasteSubjectOutsideZone.next(event);
        }
    }
}

function targetIsInputOrTextarea(event: Event): boolean {
    const target = event.target as HTMLElement;
    return ['INPUT', 'TEXTAREA'].includes(target?.tagName);
}

function getPlainTextString(event: ClipboardEvent): Observable<string | null> {
    return new Observable((subscriber) => {
        const textItem = [...event.clipboardData.items].find(isPlainTextString);
        if (textItem) {
            textItem.getAsString((str) => {
                subscriber.next(str);
                subscriber.complete();
            });
        } else {
            subscriber.next(null);
            subscriber.complete();
        }
    });
}

function hasPlainTextString(event: ClipboardEvent): boolean {
    return [...event.clipboardData.items].some(isPlainTextString);
}

function notNull<T>(input: T | null | undefined): boolean {
    return input !== null && input !== undefined;
}

function isUrl(str: string): boolean {
    return str.startsWith('http://') || str.startsWith('https://');
}

function isPlainTextString(item: DataTransferItem): boolean {
    return item.kind === 'string' && item.type === 'text/plain';
}
