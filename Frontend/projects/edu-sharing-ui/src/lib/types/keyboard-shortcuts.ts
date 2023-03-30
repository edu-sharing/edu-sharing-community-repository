export type Modifier = 'Ctrl/Cmd' | 'Shift' | 'Alt';

export interface KeyboardShortcutCondition {
    modifiers?: Modifier[];
    keyCode: string;
    ignoreWhen?: (event: KeyboardEvent) => boolean;
}
export interface KeyboardShortcut extends KeyboardShortcutCondition {
    callback: () => void;
}
