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

    /** module name by `<category>:<value>` key, populated once from the modules-info endpoint */
    private moduleMappings: Map<string, string> | null = null;

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

    private async getAvailableBackendModule(node: Node | undefined): Promise<string | null> {
        const mappings = await this.getModuleMappings(node?.ref.repo ?? '');
        const mimeType = node?.mimetype ?? '';
        const backendModule =
            mappings.get(`mediaType:${node?.mediatype ?? ''}`) ||
            mappings.get(`remoteRepository:${node?.remote?.repository?.repositoryType ?? ''}`) ||
            mappings.get(
                `replicationSource:${
                    node?.properties?.[RestConstants.CCM_PROP_REPLICATIONSOURCE]?.[0] ?? ''
                }`,
            ) ||
            mappings.get(
                `resourceType:${
                    node?.properties?.[RestConstants.CCM_PROP_CCRESSOURCETYPE]?.[0] ?? ''
                }`,
            ) ||
            mappings.get(`mimeType:${mimeType}`) ||
            mappings.get(`mimeTypePrefix:${mimeType.split('/', 2)[0]}`) ||
            null;

        if (backendModule?.toLowerCase() === 'sodix') {
            return backendModule;
        }
        // nodes with a www url are handled by the frontend url module
        if (node?.properties?.['ccm:wwwurl']?.[0]) {
            return null;
        }
        return backendModule;
    }

    private async getModuleMappings(repoId: string): Promise<Map<string, string>> {
        if (this.moduleMappings === null) {
            const result = await firstValueFrom(this.service.getModulesInfo({ repoId }));
            const mappings = new Map<string, string>();
            result.forEach(({ name, typeMapping }) => {
                // each module is registered for exactly one mapping category, in this precedence
                if (typeMapping.type != null) {
                    mappings.set(`mediaType:${typeMapping.type}`, name);
                } else if (typeMapping.remoteRepositoryType != null) {
                    mappings.set(`remoteRepository:${typeMapping.remoteRepositoryType}`, name);
                } else if (typeMapping.resourceType != null) {
                    mappings.set(`resourceType:${typeMapping.resourceType}`, name);
                } else if (typeMapping.replicationSource != null) {
                    mappings.set(`replicationSource:${typeMapping.replicationSource}`, name);
                } else if (
                    typeMapping.mimeTypePrefix != null &&
                    typeMapping.mimeTypeSuffix != null
                ) {
                    mappings.set(
                        `mimeType:${typeMapping.mimeTypePrefix}/${typeMapping.mimeTypeSuffix}`,
                        name,
                    );
                } else if (typeMapping.mimeTypePrefix != null) {
                    mappings.set(`mimeTypePrefix:${typeMapping.mimeTypePrefix}`, name);
                }
            });
            this.moduleMappings = mappings;
        }
        return this.moduleMappings;
    }

    getFrontendModuleSetting(node: Node): FrontendModuleConfig {
        return (
            this.checkUrlModule(node) ?? {
                module: 'default',
                urlModuleConfig: null,
            }
        );
    }

    private checkUrlModule(node: Node): FrontendModuleConfig | null {
        const url = node.properties?.['ccm:wwwurl']?.[0] || '';
        const remoteRepositoryType = node.remote?.repository?.repositoryType?.toLowerCase() ?? '';
        const checkSerlo = (): UrlModuleConfig | null => {
            const replicationSource =
                node.properties?.[RestConstants.CCM_PROP_REPLICATIONSOURCE]?.[0] ?? '';
            console.log('check replicationSource:', replicationSource);
            if (replicationSource === 'serlo') {
                return {
                    embedding: UrlEmbeddings.SERLO,
                    externalId:
                        node.properties?.[RestConstants.CCM_PROP_REPLICATIONSOURCEID]?.[0] ?? '',
                };
            }
            return null;
        };
        const checkLearningApps = (): UrlModuleConfig | null => {
            if (remoteRepositoryType === 'learningapps') {
                return {
                    embedding: UrlEmbeddings.LEARNINGAPPS,
                    externalId: '',
                };
            }
            return null;
        };
        const checkYouTube = (): UrlModuleConfig | null => {
            if (remoteRepositoryType === 'youtube') {
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
        const checkVimeo = (): UrlModuleConfig | null => {
            if (url.includes('vimeo.com')) {
                const urlObject = new URL(url);
                const segments = urlObject.pathname.split('/').filter(Boolean);
                let externalId = segments[segments.length - 1] ?? '';
                let h = urlObject.searchParams.get('h');
                // path-form privacy hash for unlisted videos: vimeo.com/<videoId>/<hashToken>
                if (segments[0] !== 'video' && segments.length > 1) {
                    externalId = segments[0];
                    h = segments[1];
                }
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
        const checkAudio = (): UrlModuleConfig | null => {
            if (url.length > 0 && node.mimetype?.startsWith('audio')) {
                return {
                    embedding: UrlEmbeddings.AUDIO,
                    externalId: '',
                };
            }
            return null;
        };
        const checkLti13ToolObject = (): UrlModuleConfig | null => {
            if (node.aspects?.includes('ccm:ltitool_node')) {
                return {
                    embedding: UrlEmbeddings.LTI13TOOL,
                    externalId: '',
                };
            }
            return null;
        };
        const checkImage = (): UrlModuleConfig | null => {
            // pixabay and ddb images are handled by their own embeddings / modules
            const skipTypes = ['pixabay', 'ddb'];
            if (skipTypes.includes(remoteRepositoryType)) {
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
        const detectH5P = (): UrlModuleConfig | null => {
            if (url.length > 0 && url.includes('h5p.org/h5p/embed')) {
                return {
                    embedding: UrlEmbeddings.H5P,
                    externalId: '',
                };
            }
            return null;
        };
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
            if (remoteRepositoryType === 'pixabay') {
                return {
                    embedding: UrlEmbeddings.PIXABAY,
                    externalId: '',
                };
            }
            return null;
        };
        const checkOersi = (): UrlModuleConfig | null => {
            if (remoteRepositoryType === 'oersi') {
                return {
                    embedding: UrlEmbeddings.OERSI,
                    externalId: '',
                };
            }
            return null;
        };
        const checkBrockhaus = (): UrlModuleConfig | null => {
            if (remoteRepositoryType === 'brockhaus') {
                return {
                    embedding: UrlEmbeddings.BROCKHAUS,
                    externalId: '',
                };
            }
            return null;
        };
        const checkLink = (): UrlModuleConfig | null => {
            if (url.length > 0) {
                return {
                    embedding: UrlEmbeddings.LINK,
                    externalId: '',
                };
            }
            return null;
        };

        const embedding =
            checkSerlo() ??
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
