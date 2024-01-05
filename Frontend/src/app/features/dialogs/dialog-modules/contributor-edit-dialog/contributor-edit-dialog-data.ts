import { VCard } from '../../../../core-module/ui/VCard';

export type EditMode = 'lifecycle' | 'metadata';

export interface ContributorEditDialogData {
    vCard?: VCard;
    role?: string;
    position?: number;
    editMode: EditMode;
}

export interface ContributorEditDialogResult {
    vCard: VCard;
    role: string;
}
