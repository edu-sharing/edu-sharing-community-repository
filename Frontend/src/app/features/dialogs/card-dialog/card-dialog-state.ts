export type ViewMode = 'mobile' | 'default';
export type AutoSavingState = null | 'saving' | 'saved' | 'error';

export class CardDialogState {
    viewMode: ViewMode = null;
    isLoading: boolean = false;
    autoSavingState: AutoSavingState = null;
}
