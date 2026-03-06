import { CollectionListDisplayType } from './collection-list-display-type';
import { GenericNodeEntriesDisplayType } from './generic-node-entries-display-type';
import { MediaRenderingDisplayType } from './media-rendering-display-type';

export interface LayoutOption {
    ariaLabel: string;
    icon: string;
    value: CollectionListDisplayType | GenericNodeEntriesDisplayType | MediaRenderingDisplayType;
    viewValue: string;
}
