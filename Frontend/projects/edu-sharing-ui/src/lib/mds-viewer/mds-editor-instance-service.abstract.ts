import { BehaviorSubject } from 'rxjs';
import { Node } from 'ngx-edu-sharing-api';
import { Values } from '../services/search-helper.service';

export type EditorMode = 'nodes' | 'search' | 'form' | 'inline' | 'viewer';

export abstract class MdsEditorInstanceServiceAbstract {
    mdsId: string;
    editorMode: EditorMode;
    /** Current values (if not in node mode) */
    values$ = new BehaviorSubject<Values>(null);
    /** Nodes with updated and complete metadata. */
    nodes$ = new BehaviorSubject<Node[]>(null);

    abstract saveWidgetValue(widget: any): Promise<void>;

    abstract fetchDisplayValues(widget: any): Promise<void>;
}
