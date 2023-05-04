export type Modifier = 'Ctrl/Cmd' | 'Shift' | 'Alt';

export interface KeyboardShortcutCondition {
    modifiers?: Modifier[];
    keyCode: string;
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
