import { Node } from '../../../../core-module/core.module';

export interface SaveSearchDialogData {
    name: string;
    searchString: string;
}

export type SaveSearchDialogResult = Node | null;
