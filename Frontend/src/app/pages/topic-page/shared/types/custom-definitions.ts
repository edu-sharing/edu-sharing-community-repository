import type { Locale } from 'ngx-edu-sharing-api';
import { WidgetSelectOption } from './widget-select-option';
// GENERAL CONSTANTS
export const DEFAULT_AI_CONFIG_PROP: string = 'ccm:bapi_config';
export const DEFAULT_DISCIPLINE_PROP: string = 'ccm:taxonid';
export const DEFAULT_LATITUDE_PROP: string = 'cm:latitude';
export const DEFAULT_LONGITUDE_PROP: string = 'cm:longitude';
export const DEFAULT_LOCATION_PROP: string = 'virtual:location';
export const DEFAULT_OER_LICENSES: string[] = ['CC_0', 'CC_BY', 'CC_BY_SA', 'PDM'];
export const DEFAULT_PAGE_CONFIG_ASPECT: string = 'ccm:page';
export const DEFAULT_PAGE_CONFIG_PROP: string = 'ccm:page_config';
export const DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP: string = 'ccm:page_config_propagate_ref';
export const DEFAULT_PAGE_CONFIG_REF_PROP: string = 'ccm:page_config_ref';
export const DEFAULT_PAGE_VARIANT_CONFIG_ASPECT: string = 'ccm:page_variant';
export const DEFAULT_PAGE_VARIANT_CONFIG_PROP: string = 'ccm:page_variant_config';
export const DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP: string = 'ccm:page_variant_is_template';
export const DEFAULT_PAGE_VARIANT_QUERY_ID: string = 'page_variant';
export const DEFAULT_TARGET_GROUP_PROP: string = 'ccm:educationalintendedenduserrole';
export const DEFAULT_WIDGET_CONFIG_PROP: string = 'ccm:widget_config';

// GENERAL SETTINGS
export const DEFAULT_BG_COLOR: string = '#F4F4F4';
export const DEFAULT_HEADER_TEXT_BG_COLOR: string = '#FFFFFF';
export const DEFAULT_MAP_LOCATION_LOAD_LIMIT: number = 10;
export const DEFAULT_MAP_SEARCH_DEBOUNCE_TIME: number = 500;
export const DEFAULT_MAP_MARKER_BUCKET_LIMIT: number = 10;
export const DEFAULT_MAP_MARKER_NODE_LIMIT: number = 100;
export const DEFAULT_MAP_MARKER_ZOOM_LIMIT: number = 9;
export const DEFAULT_MAP_ZOOM_LIMIT: number = 6;
export const DEFAULT_MDS_WIDGET_PREFIX: string = 'virtual:';
export const DEFAULT_NUMBER_OF_STATISTIC_ITEMS: number = 8;
export const DEFAULT_PAGE_NAME_PREFIX: string = 'PAGE_';
export const DEFAULT_PAGE_VARIANT_NAME_PREFIX: string = 'PAGE_VARIANT_';
export const DEFAULT_PLACEHOLDER_IMAGE: string =
    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGAAAABgCAQAAABIkb+zAAAAZElEQVR42u3PMQ0AAAgDsE051vlxQNI6aDN5rQICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC1wJn9TABorHDZgAAAABJRU5ErkJggg==';
export const DEFAULT_PROFILING_MDS_PROPS: string[] = [
    DEFAULT_MDS_WIDGET_PREFIX + 'profiling_widget_intention',
    DEFAULT_MDS_WIDGET_PREFIX + 'profiling_widget_education_level',
];
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
    EDITORIAL_MEMBERS: 'editorial-members',
    EMPTY_PLACEHOLDER: '',
} as const;
export type WIDGET_TYPE = (typeof WIDGETS)[keyof typeof WIDGETS];
export const WIDGET_TYPE_OPTIONS: WidgetSelectOption[] = Object.entries(WIDGETS)
    .filter(([key]) => key !== 'EMPTY_PLACEHOLDER')
    .map(([key, value]) => ({
        value: value,
        viewValue: key, // Verwendet den Key als viewValue
    }));

// WIDGET-SPECIFIC CONSTANTS + SETTINGS
export const DEFAULT_AI_CONFIG_ID: string = 'topic_page_ai_default';
export const DEFAULT_AI_CHAT_COMPLETION_CONFIG_ID: string = 'topic_page_ai_chat_completion';
export const DEFAULT_AI_IMAGE_CREATE_CONFIG_ID: string = 'topic_page_ai_create_image';
export const DEFAULT_AI_CLEAR_CACHE_CONFIG_ID: string = 'topic_page_ai_clear_cache';
export const DEFAULT_AI_TEXT_WIDGET_CONFIG_ID: string = 'topic_page_ai_text_widget';
export const DEFAULT_CONTENT_TEASER_WIDGET_CONFIG_ID: string = 'topic_page_ai_content_teaser';
export const DEFAULT_TOPIC_HEADER_DESCRIPTION_WIDGET_CONFIG_ID: string =
    'topic_page_ai_topic_header_description';
export const DEFAULT_TOPIC_HEADER_IMAGE_WIDGET_CONFIG_ID: string =
    'topic_page_ai_topic_header_image';
export const DEFAULT_TOPIC_HEADER_TEXT_WIDGET_CONFIG_ID: string = 'topic_page_ai_topic_header_text';

export const DEFAULT_COLLECTION_ID_PROP: string = 'virtual:collection_id_tree';
// BREADCRUMB + EDITORIAL-MEMBERS WIDGET
/**
 * @Deprecated
 * @TODO
 */
export const PEOPLE_LRT: string =
    'http://w3id.org/openeduhub/vocabs/oehMetadatasets/a59b2fb1-f22f-4521-a4ec-e2a101dce473';
