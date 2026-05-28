import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { SelectOption } from './select-option';
import { SwimlaneBackgroundOption } from './swimlane-background-option';
import { SwimlaneBackgroundShape } from './swimlane-background-shape';
import { WidgetSelectOption } from './widget-select-option';

// GENERAL CONSTANTS
export const DEFAULT_AI_CONFIG_PROP: string = 'ccm:bapi_config';
export const DEFAULT_COLLECTION_ID_PROP: string = 'virtual:collection_id_tree';
export const DEFAULT_DISCIPLINE_PROP: string = 'ccm:taxonid';
export const DEFAULT_OER_LICENSES: string[] = ['CC_0', 'CC_BY', 'CC_BY_SA', 'PDM'];
export const DEFAULT_PAGE_CONFIG_ASPECT: string = 'ccm:page';
export const DEFAULT_PAGE_CONFIG_PROP: string = 'ccm:page_config';
export const DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP: string =
    RestConstants.CCM_PROP_PAGE_CONFIG_PROPAGATE_REF;
export const DEFAULT_PAGE_CONFIG_REF_PROP: string = RestConstants.CCM_PROP_PAGE_CONFIG_REF;
export const DEFAULT_PAGE_TEMPLATE_ID: string = '-default_template-';
export const DEFAULT_PAGE_VARIANT_CONFIG_ASPECT: string = 'ccm:page_variant';
export const DEFAULT_PAGE_VARIANT_CONFIG_PROP: string = 'ccm:page_variant_config';
export const DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP: string = 'ccm:page_variant_is_template';
export const DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP: string = 'ccm:page_variant_template_ref';
export const DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP: string =
    'ccm:page_variant_template_version';
export const DEFAULT_PAGE_VARIANT_QUERY_ID: string = 'page_variant';
export const DEFAULT_WIDGET_CONFIG_PROP: string = 'ccm:widget_config';

// GENERAL SETTINGS
export const DEFAULT_BG_COLOR: string = '#F4F4F4';
export const DEFAULT_PAGE_NAME_PREFIX: string = 'PAGE_';
export const DEFAULT_PAGE_VARIANT_NAME_PREFIX: string = 'PAGE_VARIANT_';
export const DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION: string = '1.0.0';
export const DEFAULT_ICON_PATH: string = 'assets/images/topic-page/';
export const DEFAULT_WIDGET_NAME_PREFIX: string = 'WIDGET_';

// WIDGET DEFINITIONS
export const WIDGETS = {
    COLLECTION_CHIPS: 'collection-chips',
    TOPICS_COLUMN_BROWSER: 'topics-column-browser',
    CONTENT_TEASER: 'content-teaser',
    MEDIA_RENDERING: 'media-rendering',
    TEXT_WIDGET: 'text',
    IFRAME_WIDGET: 'iframe',
    AI_TEXT_WIDGET: 'ai-text',
    EMPTY_PLACEHOLDER: '',
} as const;
export type WIDGET_TYPE = (typeof WIDGETS)[keyof typeof WIDGETS];
export const WIDGET_TYPE_OPTIONS: WidgetSelectOption[] = Object.entries(WIDGETS)
    .filter(([key]) => key !== 'EMPTY_PLACEHOLDER')
    .map(([key, value]) => ({
        value: value,
        viewValue: key,
    }));

// WIDGET-SPECIFIC CONSTANTS + SETTINGS
export const DEFAULT_AI_CONFIG_ID: string = 'topic_page_ai_default';
export const DEFAULT_AI_CHAT_COMPLETION_CONFIG_ID: string = 'topic_page_ai_chat_completion';
export const DEFAULT_AI_IMAGE_CREATE_CONFIG_ID: string = 'topic_page_ai_create_image';
export const DEFAULT_AI_CLEAR_CACHE_CONFIG_ID: string = 'topic_page_ai_clear_cache';
export const DEFAULT_AI_TEXT_WIDGET_CONFIG_ID: string = 'topic_page_ai_text_widget';
export const DEFAULT_TOPIC_HEADER_DESCRIPTION_WIDGET_CONFIG_ID: string =
    'topic_page_ai_topic_header_description';
export const DEFAULT_TOPIC_HEADER_IMAGE_WIDGET_CONFIG_ID: string =
    'topic_page_ai_topic_header_image';
export const DEFAULT_TOPIC_HEADER_TEXT_WIDGET_CONFIG_ID: string = 'topic_page_ai_topic_header_text';

// TOPIC-PAGE DEFINITIONS
export const SWIMLANE_GRID_OPTIONS: SelectOption[] = [
    {
        icon: 'rectangle',
        value: 'one_column',
        viewValue: 'ONE_COLUMN',
    },
    {
        icon: 'edu-two_columns',
        value: 'two_columns',
        viewValue: 'TWO_COLUMNS',
    },
    {
        icon: 'edu-three_columns',
        value: 'three_columns',
        viewValue: 'THREE_COLUMNS',
    },
    {
        icon: 'edu-left_side_panel',
        value: 'left_side_panel',
        viewValue: 'LEFT_SIDE_PANEL',
    },
    {
        icon: 'edu-right_side_panel',
        value: 'right_side_panel',
        viewValue: 'RIGHT_SIDE_PANEL',
    },
];

export const SWIMLANE_TYPE_OPTIONS: SelectOption[] = [
    {
        icon: 'rectangle',
        value: 'container',
        viewValue: 'CONTAINER_ELEMENT',
    },
    {
        icon: 'storage',
        value: 'accordion',
        viewValue: 'ACCORDION_ELEMENT',
    },
    {
        icon: 'anchor',
        value: 'anchor',
        viewValue: 'ANCHOR_MENU',
    },
];

export const SWIMLANE_BACKGROUND_SHAPE_OPTIONS: SwimlaneBackgroundOption[] = [
    {
        image: 'remove_selection',
        shape: SwimlaneBackgroundShape.None,
        viewValue: 'NONE',
    },
    {
        shape: SwimlaneBackgroundShape.ArrowLarge,
        viewValue: 'ARROW_LARGE',
    },
    {
        shape: SwimlaneBackgroundShape.ArrowSmall,
        viewValue: 'ARROW_SMALL',
    },
    {
        shape: SwimlaneBackgroundShape.FlagLarge,
        viewValue: 'FLAG_LARGE',
    },
    {
        shape: SwimlaneBackgroundShape.FlagSmall,
        viewValue: 'FLAG_SMALL',
    },
    {
        shape: SwimlaneBackgroundShape.RoundLarge,
        viewValue: 'ROUND_LARGE',
    },
    {
        shape: SwimlaneBackgroundShape.RoundSmall,
        viewValue: 'ROUND_SMALL',
    },
];
