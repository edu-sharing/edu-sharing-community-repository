import type { ElementRef } from '@angular/core';

export class KeyEvents {
    static eventFromInputField(event: KeyboardEvent) {
        return (
            isInputElement(event.target) &&
            [
                'text',
                'password',
                'number',
                'email',
                'tel',
                'url',
                'search',
                'date',
                'datetime',
                'datetime-local',
                'time',
                'month',
                'week',
            ].includes(event.target.type)
        );
    }

    static isChildEvent(event: Event, parent: ElementRef<Element>) {
        let target: HTMLElement = event?.target as HTMLElement;
        while (target) {
            if (target === parent.nativeElement) {
                return true;
            }
            target = target.parentElement;
        }
        return false;
    }
}

function isInputElement(target?: EventTarget): target is HTMLInputElement {
    return target && target.constructor === HTMLInputElement;
}
