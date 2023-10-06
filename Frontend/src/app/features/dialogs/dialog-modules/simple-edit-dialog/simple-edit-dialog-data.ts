import { Node } from '../../../../core-module/core.module';

export class SimpleEditDialogData {
    nodes: Node[];
    fromUpload: boolean;
}

export type SimpleEditDialogResult = Node[] | null;
