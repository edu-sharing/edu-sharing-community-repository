import { Node } from 'ngx-edu-sharing-api';

export interface ColorChangeEvent {
    color: string;
    pageVariantNode: Node;
    swimlaneIndex: number;
}
