import { Swimlane } from './swimlane';

export interface PageStructure {
    anchorItemColor?: string;
    breadcrumbNodeId?: string;
    headerNodeId?: string;
    swimlanes: Swimlane[];
    topicColor?: string;
}
