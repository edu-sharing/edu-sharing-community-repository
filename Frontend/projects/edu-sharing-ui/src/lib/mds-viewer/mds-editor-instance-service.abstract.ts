import { BehaviorSubject } from 'rxjs';
import { Node } from 'ngx-edu-sharing-api';

/**
 * single mds value to be used with currentValue (extended details about a set value)
 */
export type MdsExtendedValue = { [key: string]: MdsExtendedValueData };
export type MdsExtendedValueData = {
    enabled: boolean;
};
export type MdsExtendedValues = { [property: string]: string[] | null | MdsExtendedValue };
export type EditorMode =
    | 'nodes'
    | 'search'
    | 'form'
    | 'inline'
    | 'viewer'
    | 'searchFacetSuggestion'
    | 'valueSelection';

export abstract class MdsEditorInstanceServiceAbstract {
    mdsId: string;
    editorMode: EditorMode;
    /** Current values (if not in node mode) */
    values$ = new BehaviorSubject<MdsExtendedValues>(null);
    /** Nodes with updated and complete metadata. */
    nodes$ = new BehaviorSubject<Node[]>(null);

    // Mutable state
    shouldShowExtendedWidgets$ = new BehaviorSubject(false);

    abstract saveWidgetValue(widget: any): Promise<void>;

    abstract fetchDisplayValues(widget: any, additionalKeys?: string[]): Promise<void>;
}
