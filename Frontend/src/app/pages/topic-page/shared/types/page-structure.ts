import { Swimlane } from './swimlane';

export interface PageStructure {
    anchorItemColor?: string;
    breadcrumbNodeId?: string;
    // render-only marker (never saved): renders the breadcrumb read-only from a parent's node
    propagatedBreadcrumbNodeId?: string;
    // copy-source pointer (never saved): parent breadcrumb node to copy on materialization
    temporaryBreadcrumbNodeId?: string;
    headerNodeId?: string;
    // render-only marker (never saved): renders the header color-only from a parent's node
    propagatedHeaderNodeId?: string;
    // copy-source pointer (never saved): parent header node to reduce-copy on materialization
    temporaryHeaderNodeId?: string;
    swimlanes: Swimlane[];
    topicColor?: string;
}
