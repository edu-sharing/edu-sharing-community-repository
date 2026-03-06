import { BaseWidgetConfig } from './base-widget-config';

export interface TopicHeaderConfig extends BaseWidgetConfig {
    aiGeneratedImage?: boolean;
    textBackgroundColor?: string;
    userUploadedNodeId?: string;
}
