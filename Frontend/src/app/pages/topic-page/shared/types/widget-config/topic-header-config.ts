import { BaseWidgetConfig } from './base-widget-config';

export interface TopicHeaderConfig extends BaseWidgetConfig {
    aiGeneratedImage?: boolean;
    // members shown and managed by the custom header extension
    editorialMemberNodeIds?: string[];
    textBackgroundColor?: string;
    userUploadedNodeId?: string;
}
