import { BapiConfigObject } from './bapi-config-object';
import { WidgetConfig } from './widget-config/widget-config';

export interface WidgetConfigObject {
    widgetConfig: WidgetConfig;
    aiConfig: BapiConfigObject;
}
