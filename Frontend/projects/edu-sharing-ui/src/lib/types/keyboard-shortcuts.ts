export type Modifier = 'Ctrl/Cmd' | 'Shift' | 'Alt';

export interface KeyboardShortcutCondition {
    modifiers?: Modifier[];
    keyCode: string;
    // the character typed (KeyboardEvent.key, case-insensitive); matched instead of keyCode when
    // set, so the shortcut follows the keyboard layout rather than the key position
    key?: string;
    ignoreWhen?: (event: KeyboardEvent) => boolean;
}
export interface KeyboardShortcut extends KeyboardShortcutCondition {
    callback: () => void;
}

export function matchesShortcutCondition(
    event: KeyboardEvent,
    condition: KeyboardShortcutCondition,
): boolean {
    return (
        matchesKey(event, condition) &&
        matchesModifiers(event, condition.modifiers) &&
        !condition.ignoreWhen?.(event)
    );
}

function matchesKey(event: KeyboardEvent, condition: KeyboardShortcutCondition): boolean {
    if (condition.key !== undefined) {
        return event.key?.toLowerCase() === condition.key.toLowerCase();
    }
    return event.code === condition.keyCode;
}

function matchesModifiers(event: KeyboardEvent, modifiers: Modifier[] = []): boolean {
    return (
        modifiers.includes('Alt') === event.altKey &&
        modifiers.includes('Shift') === event.shiftKey &&
        modifiers.includes('Ctrl/Cmd') === (event.ctrlKey || event.metaKey)
    );
}
