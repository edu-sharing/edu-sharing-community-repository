import { Injectable, inject } from '@angular/core';
import {
    DEFAULT_AI_CHAT_COMPLETION_CONFIG_ID,
    DEFAULT_AI_CLEAR_CACHE_CONFIG_ID,
    DEFAULT_AI_CONFIG_ID,
    DEFAULT_AI_IMAGE_CREATE_CONFIG_ID,
    DEFAULT_AI_TEXT_WIDGET_CONFIG_ID,
    DEFAULT_TOPIC_HEADER_DESCRIPTION_WIDGET_CONFIG_ID,
    DEFAULT_TOPIC_HEADER_IMAGE_WIDGET_CONFIG_ID,
    DEFAULT_TOPIC_HEADER_TEXT_WIDGET_CONFIG_ID,
} from '../types/custom-definitions';

@Injectable({
    providedIn: 'root',
})
export class GlobalWidgetConfigService {
    private _defaultAiConfigId: string;
    private _defaultAiChatCompletionConfigId: string;
    private _defaultAiImageCreateConfigId: string;
    private _defaultAiClearCacheConfigId: string;
    private _defaultAiTextWidgetConfigId: string;
    private _defaultTopicHeaderDescriptionWidgetNodeId: string;
    private _defaultTopicHeaderImageWidgetNodeId: string;
    private _defaultTopicHeaderTextWidgetNodeId: string;

    constructor() {
        const defaultAiConfigId = inject<string>('DEFAULT_AI_CONFIG_ID' as any, { optional: true });
        const defaultAiChatCompletionConfigId = inject<string>(
            'DEFAULT_AI_CHAT_COMPLETION_CONFIG_ID' as any,
            { optional: true },
        );
        const defaultAiImageCreateConfigId = inject<string>(
            'DEFAULT_AI_IMAGE_CREATE_CONFIG_ID' as any,
            { optional: true },
        );
        const defaultAiClearCacheConfigId = inject<string>(
            'DEFAULT_AI_CLEAR_CACHE_CONFIG_ID' as any,
            { optional: true },
        );
        const defaultAiTextWidgetConfigId = inject<string>(
            'DEFAULT_AI_TEXT_WIDGET_CONFIG_ID' as any,
            { optional: true },
        );
        const defaultTopicHeaderDescriptionWidgetNodeId = inject<string>(
            'DEFAULT_TOPIC_HEADER_DESCRIPTION_WIDGET_CONFIG_ID' as any,
            { optional: true },
        );
        const defaultTopicHeaderImageWidgetNodeId = inject<string>(
            'DEFAULT_TOPIC_HEADER_IMAGE_WIDGET_CONFIG_ID' as any,
            { optional: true },
        );
        const defaultTopicHeaderTextWidgetNodeId = inject<string>(
            'DEFAULT_TOPIC_HEADER_TEXT_WIDGET_CONFIG_ID' as any,
            { optional: true },
        );

        // IDs for the global AI configs
        this._defaultAiConfigId = defaultAiConfigId || DEFAULT_AI_CONFIG_ID;
        this._defaultAiChatCompletionConfigId =
            defaultAiChatCompletionConfigId || DEFAULT_AI_CHAT_COMPLETION_CONFIG_ID;
        this._defaultAiImageCreateConfigId =
            defaultAiImageCreateConfigId || DEFAULT_AI_IMAGE_CREATE_CONFIG_ID;
        this._defaultAiClearCacheConfigId =
            defaultAiClearCacheConfigId || DEFAULT_AI_CLEAR_CACHE_CONFIG_ID;
        // specific IDs for widget (AI) configs
        this._defaultAiTextWidgetConfigId =
            defaultAiTextWidgetConfigId || DEFAULT_AI_TEXT_WIDGET_CONFIG_ID;
        this._defaultTopicHeaderDescriptionWidgetNodeId =
            defaultTopicHeaderDescriptionWidgetNodeId ||
            DEFAULT_TOPIC_HEADER_DESCRIPTION_WIDGET_CONFIG_ID;
        this._defaultTopicHeaderImageWidgetNodeId =
            defaultTopicHeaderImageWidgetNodeId || DEFAULT_TOPIC_HEADER_IMAGE_WIDGET_CONFIG_ID;
        this._defaultTopicHeaderTextWidgetNodeId =
            defaultTopicHeaderTextWidgetNodeId || DEFAULT_TOPIC_HEADER_TEXT_WIDGET_CONFIG_ID;
    }

    /**
     * Returns the ID of the global AI config node.
     */
    get defaultAiConfigId(): string {
        return this._defaultAiConfigId;
    }

    /**
     * Sets the ID of the global AI config node.
     *
     * @param value
     */
    set defaultAiConfigId(value: string) {
        this._defaultAiConfigId = value;
    }

    /**
     * Returns the ID of the global AI chat completion config node.
     */
    get defaultAiChatCompletionConfigId(): string {
        return this._defaultAiChatCompletionConfigId;
    }

    /**
     * Sets the ID of the global AI chat completion config node.
     *
     * @param value
     */
    set defaultAiChatCompletionConfigId(value: string) {
        this._defaultAiChatCompletionConfigId = value;
    }

    /**
     * Returns the ID of the global AI image create config node.
     */
    get defaultAiImageCreateConfigId(): string {
        return this._defaultAiImageCreateConfigId;
    }

    /**
     * Sets the ID of the global AI image create config node.
     *
     * @param value
     */
    set defaultAiImageCreateConfigId(value: string) {
        this._defaultAiImageCreateConfigId = value;
    }

    /**
     * Returns the ID of the global AI config node to clear the cache.
     */
    get defaultAiClearCacheConfigId(): string {
        return this._defaultAiClearCacheConfigId;
    }

    /**
     * Sets the ID of the global AI config node to clear the cache.
     *
     * @param value
     */
    set defaultAiClearCacheConfigId(value: string) {
        this._defaultAiClearCacheConfigId = value;
    }

    /**
     * Returns the default node ID of the AI text widget.
     */
    get defaultAiTextWidgetConfigId(): string {
        return this._defaultAiTextWidgetConfigId;
    }

    /**
     * Sets the default node ID of the AI text widget.
     */
    set defaultAiTextWidgetConfigId(value: string) {
        this._defaultAiTextWidgetConfigId = value;
    }

    /**
     * Returns the default node ID of the topic header description widget.
     */
    get defaultTopicHeaderDescriptionWidgetNodeId(): string {
        return this._defaultTopicHeaderDescriptionWidgetNodeId;
    }

    /**
     * Sets the default node ID of the topic header description widget.
     */
    set defaultTopicHeaderDescriptionWidgetNodeId(value: string) {
        this._defaultTopicHeaderDescriptionWidgetNodeId = value;
    }

    /**
     * Returns the default node ID of the topic header image widget.
     */
    get defaultTopicHeaderImageWidgetNodeId(): string {
        return this._defaultTopicHeaderImageWidgetNodeId;
    }

    /**
     * Sets the default node ID of the topic header image widget.
     */
    set defaultTopicHeaderImageWidgetNodeId(value: string) {
        this._defaultTopicHeaderImageWidgetNodeId = value;
    }

    /**
     * Returns the default node ID of the topic header text widget.
     */
    get defaultTopicHeaderTextWidgetNodeId(): string {
        return this._defaultTopicHeaderTextWidgetNodeId;
    }

    /**
     * Sets the default node ID of the topic header text widget.
     */
    set defaultTopicHeaderTextWidgetNodeId(value: string) {
        this._defaultTopicHeaderTextWidgetNodeId = value;
    }
}
