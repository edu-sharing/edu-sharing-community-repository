export type ViewMode = 'mobile' | 'default';
export type SavingState = null | 'saving' | 'saved' | 'error';

export class CardDialogState {
    viewMode: ViewMode = null;
    isLoading: boolean = false;
    savingState: SavingState = null;
}
