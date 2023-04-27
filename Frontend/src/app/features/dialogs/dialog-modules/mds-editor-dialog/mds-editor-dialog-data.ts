import { Node, RestConstants } from '../../../../core-module/core.module';
import { EditorMode } from '../../../mds/types/mds-types';
import { BulkBehavior, Values } from '../../../mds/types/types';

class SharedData {
    groupId?: string;
    immediatelyShowMissingRequiredWidgets? = false;
}

export class MdsEditorDialogDataNodes extends SharedData {
    nodes: Node[];
    bulkBehavior? = BulkBehavior.Default;
}

export class MdsEditorDialogDataValues extends SharedData {
    values: Values;
    repository?: string = RestConstants.HOME_REPOSITORY;
    setId: string;
    editorMode: EditorMode;
}

export function hasNodes(data: MdsEditorDialogData): data is MdsEditorDialogDataNodes {
    return !!(data as MdsEditorDialogDataNodes).nodes;
}

export function hasValues(data: MdsEditorDialogData): data is MdsEditorDialogDataValues {
    return !!(data as MdsEditorDialogDataValues).values;
}

export type MdsEditorDialogData = MdsEditorDialogDataNodes | MdsEditorDialogDataValues;

export type MdsEditorDialogResultNodes = Node[] | null;
export type MdsEditorDialogResultValues = Values | null;
export type MdsEditorDialogResult = MdsEditorDialogResultNodes | MdsEditorDialogResultValues;
