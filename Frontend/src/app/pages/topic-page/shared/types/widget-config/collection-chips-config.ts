import { BaseWidgetConfig } from './base-widget-config';
import { CollectionListDisplayType } from '../collection-list-display-type';

export interface CollectionChipsConfig extends BaseWidgetConfig {
    collectionListLayout?: CollectionListDisplayType;
    sortedNodeIds?: string[];
}
