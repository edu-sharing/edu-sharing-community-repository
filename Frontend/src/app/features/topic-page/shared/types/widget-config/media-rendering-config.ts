import { BaseWidgetConfig } from './base-widget-config';
import { MediaRenderingDisplayType } from '../media-rendering-display-type';

export interface MediaRenderingConfig extends BaseWidgetConfig {
    mediaRenderingLayout?: MediaRenderingDisplayType;
    selectedNodeId?: string;
}
