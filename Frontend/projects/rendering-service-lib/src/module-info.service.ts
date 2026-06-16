import { Injectable, inject } from '@angular/core';
import { ModuleInfoControllerService } from 'ngx-rendering-service-api';
import { firstValueFrom } from 'rxjs';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import {
    FrontendModuleConfig,
    UrlEmbeddings,
    UrlModuleConfig,
} from './lib/dto/FrontendModuleConfig';
import { ModuleInfo } from './lib/dto/ModuleInfo';

@Injectable({ providedIn: 'root' })
export class ModuleInfoService {
    private service = inject(ModuleInfoControllerService);

    private availableMediaTypes: Map<string, string> = new Map<string, string>();
    private availableMimeTypes: Map<string, string> = new Map<string, string>();
    private availableMimeTypePrefixes: Map<string, string> = new Map<string, string>();
    private availableReplicationSources: Map<string, string> = new Map<string, string>();
    private availableResourceTypes: Map<string, string> = new Map<string, string>();
    private availableRemoteRepositories: Map<string, string> = new Map<string, string>();

    async getModuleInfo(node: Node): Promise<ModuleInfo> {
        const backendModule = await this.getAvailableBackendModule(node);
        if (backendModule !== null) {
            return {
                module: backendModule.toLowerCase(),
                urlType: null,
            };
        }
        const frontendModule = this.getFrontendModuleSetting(node);
        return {
            module: frontendModule.module,
            urlType: frontendModule.urlModuleConfig?.embedding ?? null,
        };
    }

    async getAvailableBackendModule(node: Node | undefined): Promise<string | null> {
        if (!this.isInitialized()) {
            const result = await firstValueFrom(
                this.service.getModulesInfo({ repoId: node?.ref.repo ?? '' }),
            );
            result.forEach((info) => {
                if (info.typeMapping.type !== undefined && info.typeMapping.type !== null) {
                    this.availableMediaTypes.set(info.typeMapping.type, info.name);
                } else if (
                    info.typeMapping.remoteRepositoryType !== undefined &&
                    info.typeMapping.remoteRepositoryType !== null
                ) {
                    this.availableRemoteRepositories.set(
                        info.typeMapping.remoteRepositoryType,
                        info.name,
                    );
                } else if (
                    info.typeMapping.resourceType !== undefined &&
                    info.typeMapping.resourceType !== null
                ) {
                    this.availableResourceTypes.set(info.typeMapping.resourceType, info.name);
                } else if (
                    info.typeMapping.replicationSource !== undefined &&
                    info.typeMapping.replicationSource !== null
                ) {
                    this.availableReplicationSources.set(
                        info.typeMapping.replicationSource,
                        info.name,
                    );
                } else if (
                    info.typeMapping.mimeTypeSuffix !== undefined &&
                    info.typeMapping.mimeTypeSuffix !== null &&
                    info.typeMapping.mimeTypePrefix !== undefined &&
                    info.typeMapping.mimeTypePrefix !== null
                ) {
                    this.availableMimeTypes.set(
                        `${info.typeMapping.mimeTypePrefix}/${info.typeMapping.mimeTypeSuffix}`,
                        info.name,
                    );
                } else if (
                    info.typeMapping.mimeTypePrefix !== undefined &&
                    info.typeMapping.mimeTypePrefix !== null
                ) {
                    this.availableMimeTypePrefixes.set(info.typeMapping.mimeTypePrefix, info.name);
                }
            });
        }
        const mimeTypePrefix = (node?.mimetype ?? '').split('/', 2)[0];
        const backendModule =
            this.availableMediaTypes.get(node?.mediatype ?? '') ||
            this.availableRemoteRepositories.get(node?.remote?.repository?.repositoryType ?? '') ||
            this.availableReplicationSources.get(
                node?.properties?.[RestConstants.CCM_PROP_REPLICATIONSOURCE]?.[0] ?? '',
            ) ||
            this.availableResourceTypes.get(
                node?.properties?.[RestConstants.CCM_PROP_CCRESSOURCETYPE]?.[0] ?? '',
            ) ||
            this.availableMimeTypes.get(node?.mimetype ?? '') ||
            this.availableMimeTypePrefixes.get(mimeTypePrefix) ||
            null;

        if (backendModule?.toLowerCase() === 'sodix') {
            return backendModule;
        }
        if (node?.properties?.['ccm:wwwurl']?.[0]) {
            return null;
        } else {
            return backendModule;
        }
    }

    private isInitialized(): boolean {
        return (
            this.availableMediaTypes.size +
                this.availableMimeTypes.size +
                this.availableMimeTypePrefixes.size +
                this.availableReplicationSources.size +
                this.availableResourceTypes.size +
                this.availableRemoteRepositories.size !==
            0
        );
    }

    getFrontendModuleSetting(node: Node): FrontendModuleConfig {
        const urlModuleConfig = this.checkUrlModule(node);
        if (urlModuleConfig !== null) {
            return urlModuleConfig;
        }
        return {
            module: 'default',
            urlModuleConfig: null,
        };
    }

    checkUrlModule(node: Node): FrontendModuleConfig | null {
        const url = node.properties?.['ccm:wwwurl']?.[0] || '';
        const checkLearningApps = (): UrlModuleConfig | null => {
            if (node.remote?.repository?.repositoryType?.toLowerCase() === 'learningapps') {
                return {
                    embedding: UrlEmbeddings.LEARNINGAPPS,
                    externalId: '',
                };
            }
            return null;
        };
        /**
         * Function checkYouTube
         */
        const checkYouTube = (): UrlModuleConfig | null => {
            if (node.remote?.repository?.repositoryType?.toLowerCase() === 'youtube') {
                return {
                    embedding: UrlEmbeddings.YOUTUBE,
                    externalId: node.remote?.id ?? '',
                };
            }
            if (url.includes('youtu.be')) {
                const split = url.split('/');
                return {
                    embedding: UrlEmbeddings.YOUTUBE,
                    externalId: split[split.length - 1],
                };
            }
            if (url.includes('.youtube.com/watch?')) {
                const urlObject = new URL(url);
                return {
                    embedding: UrlEmbeddings.YOUTUBE,
                    externalId: urlObject.searchParams.get('v') ?? '',
                };
            }
            return null;
        };
        /**
         * Function checkVimeo
         */
        const checkVimeo = (): UrlModuleConfig | null => {
            if (url.includes('vimeo.com')) {
                const urlObject = new URL(url);
                const segments = urlObject.pathname.split('/').filter(Boolean);
                let externalId = segments[segments.length - 1] ?? '';
                const h = urlObject.searchParams.get('h');
                if (h !== null) {
                    externalId += `?h=${h}`;
                }
                return {
                    embedding: UrlEmbeddings.VIMEO,
                    externalId,
                };
            }
            return null;
        };
        const checkGenericVideo = (): UrlModuleConfig | null => {
            if (url.length > 0 && ['mp4', 'webm'].includes(url.split('.').pop() ?? '')) {
                return {
                    embedding: UrlEmbeddings.VIDEO,
                    externalId: '',
                };
            }
            return null;
        };
        /**
         * Function checkAudio
         */
        const checkAudio = (): UrlModuleConfig | null => {
            if (url.length > 0 && node.mimetype?.startsWith('audio')) {
                return {
                    embedding: UrlEmbeddings.AUDIO,
                    externalId: '',
                };
            }
            return null;
        };
        /**
         * Function checkLti13ToolObject
         */
        const checkLti13ToolObject = (): UrlModuleConfig | null => {
            if (node.aspects?.includes('ccm:ltitool_node')) {
                return {
                    embedding: UrlEmbeddings.LTI13TOOL,
                    externalId: '',
                };
            }
            return null;
        };
        /**
         * Function checkImage
         */
        const checkImage = (): UrlModuleConfig | null => {
            const remoteRepository = node.remote?.repository?.repositoryType ?? '';
            const skipTypes = ['PIXABAY', 'DDB'];
            if (skipTypes.includes(remoteRepository)) {
                return null;
            }
            if (node.mediatype === 'file-image') {
                return {
                    embedding: UrlEmbeddings.IMAGE,
                    externalId: '',
                };
            }
            const mimeTypeSplit = node.mimetype?.split('/') ?? [];
            if (
                mimeTypeSplit.length === 2 &&
                ['png', 'jpg', 'jpeg', 'gif'].includes(mimeTypeSplit[1])
            ) {
                return {
                    embedding: UrlEmbeddings.IMAGE,
                    externalId: '',
                };
            }
            return null;
        };
        /**
         *  Function detectH5P
         */
        const detectH5P = (): UrlModuleConfig | null => {
            if (url.length > 0 && url.includes('h5p.org/h5p/embed')) {
                return {
                    embedding: UrlEmbeddings.H5P,
                    externalId: '',
                };
            }
            return null;
        };
        /**
         * Function detectPrezi
         */
        const detectPrezi = (): UrlModuleConfig | null => {
            if (
                url.length > 0 &&
                (url.includes('prezi.com/view/') ||
                    url.includes('prezi.com/embed/') ||
                    url.includes('prezi.com/p/embed'))
            ) {
                return {
                    embedding: UrlEmbeddings.PREZI,
                    externalId: '',
                };
            }
            return null;
        };

        const checkPixabay = (): UrlModuleConfig | null => {
            const remoteType = node.remote?.repository?.repositoryType ?? '';
            if (remoteType.toLowerCase() === 'pixabay') {
                return {
                    embedding: UrlEmbeddings.PIXABAY,
                    externalId: '',
                };
            }
            return null;
        };

        const checkOersi = (): UrlModuleConfig | null => {
            const remoteType = node.remote?.repository?.repositoryType ?? '';
            if (remoteType.toLowerCase() === 'oersi') {
                return {
                    embedding: UrlEmbeddings.OERSI,
                    externalId: '',
                };
            }
            return null;
        };

        const checkBrockhaus = (): UrlModuleConfig | null => {
            const remoteType = node.remote?.repository?.repositoryType ?? '';
            if (remoteType.toLowerCase() === 'brockhaus') {
                return {
                    embedding: UrlEmbeddings.BROCKHAUS,
                    externalId: '',
                };
            }
            return null;
        };

        /**
         * Function checkLink
         */
        const checkLink = (): UrlModuleConfig | null => {
            if (url.length > 0 && node.mediatype === 'link') {
                return {
                    embedding: UrlEmbeddings.LINK,
                    externalId: '',
                };
            }
            return null;
        };

        /**
         * embedding if applicable to the node
         */
        const embedding =
            checkLearningApps() ??
            checkYouTube() ??
            checkVimeo() ??
            checkGenericVideo() ??
            checkAudio() ??
            checkImage() ??
            checkLti13ToolObject() ??
            detectH5P() ??
            detectPrezi() ??
            checkPixabay() ??
            checkOersi() ??
            checkBrockhaus() ??
            checkLink();

        if (embedding !== null) {
            return {
                module: 'url',
                urlModuleConfig: embedding,
            };
        }
        return null;
    }
}
