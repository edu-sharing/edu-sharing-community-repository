import { BaseWidgetConfig } from './base-widget-config';
import { TextVariant } from '../text-variant';

export interface AiTextWidgetConfig extends BaseWidgetConfig {
    prompt?: string;
    texts?: TextVariant[];
}
