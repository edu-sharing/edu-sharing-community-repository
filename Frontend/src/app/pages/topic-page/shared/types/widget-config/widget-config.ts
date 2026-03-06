import { AiTextWidgetConfig } from './ai-text-widget-config';
import { BreadcrumbConfig } from './breadcrumb-config';
import { CollectionChipsConfig } from './collection-chips-config';
import { ContentTeaserConfig } from './content-teaser-config';
import { IframeWidgetConfig } from './iframe-widget-config';
import { MediaRenderingConfig } from './media-rendering-config';
import { TopicHeaderConfig } from './topic-header-config';

export type WidgetConfig =
    | AiTextWidgetConfig
    | BreadcrumbConfig
    | CollectionChipsConfig
    | ContentTeaserConfig
    | IframeWidgetConfig
    | MediaRenderingConfig
    | TopicHeaderConfig;
