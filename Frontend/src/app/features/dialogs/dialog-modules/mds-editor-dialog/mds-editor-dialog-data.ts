import { Node } from '../../../../core-module/core.module';
import { BulkBehavior } from '../../../mds/types/types';

export class MdsEditorDialogData {
    nodes: Node[];
    bulkBehavior? = BulkBehavior.Default;
    immediatelyShowMissingRequiredWidgets? = false;
}

export type MdsEditorDialogResult = Node[] | null;
