export type HistoryAction = 'undo' | 'redo';

interface HistoryShortcut {
    key: string;
    shiftKey: boolean;
    action: HistoryAction;
}

// matched by the character typed, not the key position, so they follow the keyboard layout
const HISTORY_SHORTCUTS: HistoryShortcut[] = [
    { key: 'z', shiftKey: false, action: 'undo' },
    { key: 'z', shiftKey: true, action: 'redo' },
    { key: 'y', shiftKey: false, action: 'redo' },
];

/**
 * The history action a key press asks for (Ctrl/Cmd+Z, Ctrl/Cmd+Shift+Z or Ctrl/Cmd+Y), if any.
 */
export function historyActionForKey(event: KeyboardEvent): HistoryAction | null {
    if (!(event.ctrlKey || event.metaKey) || event.altKey) {
        return null;
    }
    const key: string = event.key?.toLowerCase();
    const shortcut: HistoryShortcut | undefined = HISTORY_SHORTCUTS.find(
        (candidate: HistoryShortcut) =>
            candidate.key === key && candidate.shiftKey === event.shiftKey,
    );
    return shortcut?.action ?? null;
}
