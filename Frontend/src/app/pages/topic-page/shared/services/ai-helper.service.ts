import { Injectable, inject } from '@angular/core';
import { AboutService, FeaturesHelperService, UserService } from 'ngx-edu-sharing-api';
import {
    CreateChatCompletionResponse,
    EduSharingLlmService,
    ImagesResponse,
    MdsConfig,
    NodeConfig,
} from 'ngx-edu-sharing-b-api';
import { firstValueFrom } from 'rxjs';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';
import { retrieveMdsConfig } from '../utils/ai-util';
import { GlobalWidgetConfigService } from './global-widget-config.service';

@Injectable({
    providedIn: 'root',
})
export class AiHelperService {
    private aboutService = inject(AboutService);
    private eduSharingLlmService = inject(EduSharingLlmService);
    private featuresHelperService = inject(FeaturesHelperService);
    private genericWidgetGlobalService = inject(GenericWidgetGlobalService);
    private globalWidgetConfigService = inject(GlobalWidgetConfigService);
    private userService = inject(UserService);

    /**
     * Checks whether AI is supported.
     */
    async hasAISupport(): Promise<boolean> {
        return await this.featuresHelperService.hasUserAISupport();
    }

    /**
     * Checks whether rendering service 2 is supported.
     */
    async hasRendering2Support(): Promise<boolean> {
        const about = await firstValueFrom(this.aboutService.getAbout());
        return !!about.renderingService2;
    }

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
    ): Promise<CreateChatCompletionResponse | null> {
        const user: string = await this.getCurrentUser();
        let config: NodeConfig | MdsConfig;
        if (typeof configId === 'string') {
            config = retrieveMdsConfig(configId);
        } else {
            config = configId;
        }
        // a failing prompt must not break the rendering of the surrounding page,
        // so errors (e.g. a missing or expired B-API authorization) resolve to no result
        try {
            return await firstValueFrom(
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
        } catch (error) {
            console.warn('AI text generation failed', error);
            return null;
        }
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
    ): Promise<ImagesResponse> {
        const user: string = await this.getCurrentUser();
        return firstValueFrom(
            this.eduSharingLlmService.imageGeneration({
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
    ): Promise<ImagesResponse> {
        const user: string = await this.getCurrentUser();
        return firstValueFrom(
            this.eduSharingLlmService.imageGeneration({
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
