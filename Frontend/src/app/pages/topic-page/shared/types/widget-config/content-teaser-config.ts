import { Values } from 'ngx-edu-sharing-ui';
import { GenericNodeEntriesDisplayType } from '../generic-node-entries-display-type';
import { BaseWidgetConfig } from './base-widget-config';

export interface ContentTeaserConfig extends BaseWidgetConfig {
    blacklistedNodeIds?: string[];
    contentTeaserLayout?: GenericNodeEntriesDisplayType;
    propertyFilters?: Values;
    searchText?: string;
}
