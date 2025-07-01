import {
    DashboardShortcut,
    DefaultDashboardShortcut,
    DefaultDashboardShortcutEntry,
    RefDashboardShortcut,
    RefDashboardShortcutEntry,
} from 'ngx-edu-sharing-api';

// extend existing types by additional attributes used to simplify computation
type DefaultOrRefShortcutEntry = Partial<DefaultDashboardShortcutEntry> &
    Partial<RefDashboardShortcutEntry>;
export type ExtendedShortcutEntry = DefaultOrRefShortcutEntry & {
    icon?: string;
    url?: string;
    updates?: number;
};
// TODO: why is "DashboardShortcut &" necessary here?
export type DefaultOrRefShortcut = DashboardShortcut &
    Partial<DefaultDashboardShortcut> &
    Partial<RefDashboardShortcut>;
