export class AccessibilitySettings {
    toastMode: 'important' | 'all' = 'all';
    toastDuration: ToastDuration = ToastDuration.Seconds_5;
    contrastMode = false;
    indicatorIcons = true;
}
export enum ToastDuration {
    Seconds_3 = 3,
    Seconds_5 = 5,
    Seconds_8 = 8,
    Seconds_15 = 15,
    Seconds_30 = 30,
    Seconds_60 = 60,
    Infinite = null,
}
