import { CreateChatCompletionResponse, MdsConfig } from 'ngx-edu-sharing-b-api';
import { BapiChatCompletionConfig } from '../types/bapi-chat-completion-config';
import { BapiConfigObject } from '../types/bapi-config-object';

/**
 * Returns the message content of the first choices of a given completion result.
 * An absent result (e.g. a failed generation) yields an empty string.
 *
 * @param completionResult
 */
export const retrieveResultString = (
    completionResult: CreateChatCompletionResponse | null,
): string => {
    return completionResult?.choices?.[0]?.message?.content ?? '';
};

/**
 * Returns, whether a given string contains AI tags or not.
 */
export const containsAiTags = (text: string): boolean => {
    if (!text) {
        return false;
    }
    return (
        text.includes('{{') &&
        text.includes('}}') &&
        (text.includes('node(') || text.includes('var('))
    );
};

/**
 * Returns the BAPI config object with a given description and headline.
 *
 * @param description
 * @param headline
 * @param customKeyValues
 */
export const retrieveBapiConfigObject = (
    description?: string,
    headline?: string,
    customKeyValues?: { [key: string]: string },
): BapiConfigObject => {
    const configObject: BapiConfigObject = {};
    if (description) {
        configObject.description = retrieveChatCompletionObject(description);
    }
    if (headline) {
        configObject.headline = retrieveChatCompletionObject(headline);
    }
    if (customKeyValues) {
        Object.keys(customKeyValues).forEach((key) => {
            configObject[key] = retrieveChatCompletionObject(customKeyValues[key]);
        });
    }
    return configObject;
};

/**
 * Returns the BAPI config with a given textual content.
 *
 * @param content
 */
export const retrieveChatCompletionObject = (content: string): BapiChatCompletionConfig => {
    if (!content) {
        return {};
    }
    return {
        prompt: {
            messages: [
                {
                    role: 'user',
                    content,
                },
            ],
        },
    };
};

/**
 * Returns the MDS config with a given id.
 *
 * @param id
 */
export const retrieveMdsConfig = (id: string): MdsConfig => {
    return {
        type: 'mds',
        id,
    };
};
