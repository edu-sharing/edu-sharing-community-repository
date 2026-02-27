import { ContentTeaserConfig } from './content-teaser-config';
import { AiTextWidgetConfig } from './ai-text-widget-config';
import { TopicHeaderConfig } from './topic-header-config';
import { MediaRenderingConfig } from './media-rendering-config';
import { EditorialMembersConfig } from './editorial-members-config';
import { CollectionChipsConfig } from './collection-chips-config';
import { BreadcrumbConfig } from './breadcrumb-config';
import { IframeWidgetConfig } from './iframe-widget-config';

export type WidgetConfig =
    | AiTextWidgetConfig
    | BreadcrumbConfig
    | CollectionChipsConfig
    | ContentTeaserConfig
    | EditorialMembersConfig
    | IframeWidgetConfig
    | MediaRenderingConfig
    | TopicHeaderConfig;
