interface PromptMessage {
    role: string;
    content: string;
}
interface PromptObject {
    messages: PromptMessage[];
}
export interface BapiChatCompletionConfig {
    clearCache?: boolean;
    id?: string;
    prompt?: PromptObject;
    provider?: string;
    useCaching?: boolean;
}
