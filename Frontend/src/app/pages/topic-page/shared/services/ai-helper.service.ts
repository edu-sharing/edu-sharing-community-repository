import { Injectable } from '@angular/core';
import { UserService } from 'ngx-edu-sharing-api';
import {
    ChatCompletionResult,
    EduSharingLlmService,
    ImageResult,
    MdsConfig,
    NodeConfig,
} from 'ngx-edu-sharing-b-api';
import { firstValueFrom } from 'rxjs';
import { retrieveMdsConfig } from '../utils/ai-util';
import { GlobalWidgetConfigService } from './global-widget-config.service';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';

@Injectable({
    providedIn: 'root',
})
export class AiHelperService {
    constructor(
        private userService: UserService,
        private eduSharingLlmService: EduSharingLlmService,
        private globalWidgetConfigService: GlobalWidgetConfigService,
        private genericWidgetGlobalService: GenericWidgetGlobalService,
    ) {}

    /**
     * Helper function to call the B-API to generate a text from a prompt stored in a node with a given ID.
     *
     * @param configId
     * @param variables
     * @param contextNodeId
     */
    async generateFromPrompt(
        configId: string | NodeConfig,
        variables: { [key: string]: string[] } = {},
        contextNodeId: string,
    ): Promise<ChatCompletionResult> {
        const user: string = await this.getCurrentUser();
        let config: NodeConfig | MdsConfig;
        if (typeof configId === 'string') {
            config = retrieveMdsConfig(configId);
        } else {
            config = configId;
        }
        return firstValueFrom(
            this.eduSharingLlmService.chatCompletions({
                body: {
                    configIds: [
                        retrieveMdsConfig(this.globalWidgetConfigService.defaultAiConfigId),
                        retrieveMdsConfig(
                            this.globalWidgetConfigService.defaultAiChatCompletionConfigId,
                        ),
                        config,
                    ],
                    contextNodeId,
                    metadataSet: this.genericWidgetGlobalService.getDefaultMds(),
                    user,
                    variables,
                },
            }),
        );
    }

    /**
     * Calls the B-API to generate an image from a prompt stored in a node with a given ID.
     *
     * @param widgetNodeId
     * @param contextNodeId
     * @param variables
     */
    async createAiImage(
        widgetNodeId: string,
        contextNodeId: string,
        variables: { [key: string]: string[] } = {},
    ): Promise<ImageResult> {
        const user: string = await this.getCurrentUser();
        return firstValueFrom(
            this.eduSharingLlmService.imageGeneration1({
                body: {
                    configIds: [
                        retrieveMdsConfig(this.globalWidgetConfigService.defaultAiConfigId),
                        retrieveMdsConfig(
                            this.globalWidgetConfigService.defaultAiImageCreateConfigId,
                        ),
                        retrieveMdsConfig(widgetNodeId),
                    ],
                    contextNodeId,
                    metadataSet: this.genericWidgetGlobalService.getDefaultMds(),
                    user,
                    variables,
                },
            }),
        );
    }

    /**
     * Calls the B-API to regenerate an image from a prompt stored in a node with a given ID.
     *
     * @param widgetNodeId
     * @param contextNodeId
     * @param variables
     */
    async updateAiImage(
        widgetNodeId: string,
        contextNodeId: string,
        variables: { [key: string]: string[] } = {},
    ): Promise<ImageResult> {
        const user: string = await this.getCurrentUser();
        return firstValueFrom(
            this.eduSharingLlmService.imageGeneration1({
                body: {
                    configIds: [
                        retrieveMdsConfig(this.globalWidgetConfigService.defaultAiConfigId),
                        retrieveMdsConfig(
                            this.globalWidgetConfigService.defaultAiImageCreateConfigId,
                        ),
                        retrieveMdsConfig(widgetNodeId),
                        retrieveMdsConfig(
                            this.globalWidgetConfigService.defaultAiClearCacheConfigId,
                        ),
                    ],
                    contextNodeId,
                    metadataSet: this.genericWidgetGlobalService.getDefaultMds(),
                    user,
                    variables,
                },
            }),
        );
    }

    /**
     * Helper function to retrieve the current username.
     */
    async getCurrentUser(): Promise<string> {
        const loginInfo = await firstValueFrom(this.userService.observeCurrentUser());
        return loginInfo?.person.authorityName;
    }
}
