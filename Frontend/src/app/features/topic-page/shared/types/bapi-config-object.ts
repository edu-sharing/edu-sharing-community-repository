import { BapiChatCompletionConfig } from './bapi-chat-completion-config';

export interface BapiConfigObject {
    headline?: BapiChatCompletionConfig;
    description?: BapiChatCompletionConfig;
    [key: string]: BapiChatCompletionConfig | undefined;
}
