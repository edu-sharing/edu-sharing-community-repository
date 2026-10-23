import { BaseWidgetConfig } from './base-widget-config';

/**
 * Config of the breadcrumb widget nodes older page variants carry; only read for the editorial
 * members the topic header did not hold yet.
 */
export interface BreadcrumbConfig extends BaseWidgetConfig {
    editorialMemberNodeIds?: string[];
}
