import { Node, NodeEntries } from 'ngx-edu-sharing-api';
import type {
    CopyOption,
    PageVariantSearchState,
} from '../../editor/add-page-variant-or-template-dialog/add-page-variant-or-template-dialog.component';
import { PromptToTextMapping } from './prompt-to-text-mapping';
import { Swimlane } from './swimlane';

export type CreationKind = 'variant' | 'template';

/**
 * Nodes listed by the dialog for creating a page variant or template, shown one at a time in place
 * of the collection's page.
 */
export interface VariantPreview {
    kind: CreationKind;
    // where the nodes were listed, which decides how a page is created from one of them
    copyOption: CopyOption;
    nodes: Node[];
    index: number;
    // the dialog search the nodes were listed by, to continue with when returning to the dialog
    searchState: PageVariantSearchState | null;
}

/**
 * The state of the collection's page a preview replaces, restored as it was once the preview ends.
 */
export interface VariantPreviewRestoreState {
    anchorItemColor: string;
    breadcrumbNodeId: string;
    editMode: boolean;
    headerNodeId: string;
    pageConfigCheckFailed: boolean;
    pageVariantConfigs: NodeEntries;
    pageVariantNode: Node | null;
    pageVariantNodeIndex: number;
    pageVariantSettingsValid: boolean;
    persistedSwimlanes: Swimlane[];
    propagatedBreadcrumbNodeId: string;
    propagatedHeaderNodeId: string;
    scrollTop: number;
    selectedVariables: string;
    selectedVariantPosition: number;
    swimlaneIdToPromptTextMapping: Map<string, PromptToTextMapping>;
    swimlanes: Swimlane[];
    topicColor: string;
}
