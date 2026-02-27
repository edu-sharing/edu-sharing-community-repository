import { ChatCompletionRequest, CreateImageRequest } from 'ngx-edu-sharing-b-api';

export interface BapiConfig {
    chatCompletion?: ChatCompletionRequest;
    clearCache?: boolean;
    createImage?: CreateImageRequest;
    provider?: string;
    useCaching?: boolean;
}
