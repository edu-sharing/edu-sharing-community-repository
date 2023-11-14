import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';
import { KeyEvents } from '../util/key-events';
import { DialogsService } from '../features/dialogs/dialogs.service';
import { CardComponent } from '../shared/components/card/card.component';
import { KeyboardShortcut, KeyboardShortcutCondition, Modifier } from 'ngx-edu-sharing-ui';

interface ShortcutsRecord {
    shortcuts: KeyboardShortcut[];
}

@Injectable({ providedIn: 'root' })
export class KeyboardShortcutsService implements KeyboardShortcutsService {
    private shortcuts: ShortcutsRecord[] = [];
    private flattenedShortcuts: KeyboardShortcut[] = [];

    constructor(private dialogs: DialogsService, private ngZone: NgZone) {
        this.ngZone.runOutsideAngular(() =>
            document.addEventListener('keydown', (event) => this.handleKeydown(event)),
        );
    }

    register(shortcuts: KeyboardShortcut[], { until }: { until: Observable<void> }) {
        const record = { shortcuts };
        this.shortcuts.push(record);
        this.updateFlattenedShortcuts();
        until.pipe(take(1)).subscribe(() => this.unregister(record));
    }

    shouldIgnoreShortcut(event: KeyboardEvent): boolean {
        return (
            KeyEvents.eventFromInputField(event) ||
            // Do nothing if a modal dialog is still open.
            //
            // FIXME: this doesn't allow us to use any shortcuts in dialogs.
            CardComponent.getNumberOfOpenCards() > 0 ||
            this.dialogs.openDialogs.length > 0
        );
    }

    private updateFlattenedShortcuts() {
        this.flattenedShortcuts = this.shortcuts.reduce(
            (acc, record) => [...acc, ...record.shortcuts],
            [],
        );
    }

    private unregister(record: ShortcutsRecord) {
        const index = this.shortcuts.indexOf(record);
        if (index >= 0) {
            this.shortcuts.splice(index, 1);
            this.updateFlattenedShortcuts();
        } else {
            throw new Error('Failed to unregister keyboard shortcuts');
        }
    }

    private handleKeydown(event: KeyboardEvent) {
        if (this.shouldIgnoreShortcut(event)) {
            return;
        }
        for (const shortcut of this.flattenedShortcuts) {
            if (matchesShortcutCondition(event, shortcut)) {
                event.preventDefault();
                event.stopPropagation();
                this.ngZone.run(() => shortcut.callback());
                return;
            }
        }
    }
}

export function matchesShortcutCondition(
    event: KeyboardEvent,
    condition: KeyboardShortcutCondition,
): boolean {
    return (
        event.code === condition.keyCode &&
        matchesModifiers(event, condition.modifiers) &&
        !condition.ignoreWhen?.(event)
    );
}

function matchesModifiers(event: KeyboardEvent, modifiers: Modifier[] = []): boolean {
    return (
        modifiers.includes('Alt') === event.altKey &&
        modifiers.includes('Shift') === event.shiftKey &&
        modifiers.includes('Ctrl/Cmd') === (event.ctrlKey || event.metaKey)
    );
}
