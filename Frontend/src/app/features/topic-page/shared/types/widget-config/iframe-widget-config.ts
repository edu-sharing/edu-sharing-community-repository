import { BaseWidgetConfig } from './base-widget-config';

export interface IframeWidgetConfig extends BaseWidgetConfig {
    src?: string;
    width?: number | string;
    height?: number | string;
    border?: boolean;
    confirmation?: boolean;
    title?: string;
}
