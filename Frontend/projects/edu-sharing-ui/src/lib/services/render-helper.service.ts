import { Injectable, Injector, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { RenderDataRequestWithToken, RSApiConfiguration } from 'ngx-rendering-service-api';
import {
    AboutService,
    ConfigService,
    HOME_REPOSITORY,
    Node,
    NodeService,
    NodeServiceUnwrapped,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';
import { OptionsHelperDataService } from './options-helper-data.service';
import { Scope } from '../types/option-item';

export type CombinedRenderData = {
    node: Node;
    /**
     * the parent node; only set if the current element is a series (child)
     */
    nodeParent?: Node;
    request?: RenderDataRequestWithToken;
    error?: string;
};
@Injectable({ providedIn: 'root' })
export class RenderHelperService {
    private injector = inject(Injector);
    private aboutService = inject(AboutService);
    private configService = inject(ConfigService);
    private nodeService = inject(NodeService);
    private nodeApiUnwrapped = inject(NodeServiceUnwrapped);
    private configuration = inject(EduSharingUiConfiguration);
    private optionsHelperDataService = inject(OptionsHelperDataService, { optional: true });

    async getRenderData(
        nodeId: string,
        version: string = null,
        repository = HOME_REPOSITORY,
    ): Promise<CombinedRenderData> {
        const about = await firstValueFrom(this.aboutService.getAbout());
        const securedNode = await firstValueFrom(
            this.nodeApiUnwrapped.getMetadataSigned({
                repository: repository || HOME_REPOSITORY,
                node: nodeId,
                version: version || RestConstants.NODE_VERSION_CURRENT,
            }),
        );
        let node = securedNode.node;
        let nodeParent: Node = null;
        if (node.aspects?.includes(RestConstants.CCM_ASPECT_IO_CHILDOBJECT)) {
            nodeParent = await this.inheritProps(node);
        }
        this.optionsHelperDataService?.setData({
            scope: Scope.Render,
            activeObjects: [node],
            parent: {
                ref: {
                    id: node.parent.id,
                },
            },
            customOptions: {
                useDefaultOptions: true,
            },
            postPrepareOptions: (options, objects) => {
                if (version && version !== RestConstants.NODE_VERSION_CURRENT) {
                    options.filter((o) => o.name === 'OPTIONS.OPEN')[0].isEnabled = false;
                }
            },
        });
        await this.optionsHelperDataService?.refreshComponents();
        if (!about.renderingService2) {
            console.error('no rendering service 2 url was configured. Will not continue.');
            return {
                node,
                error: 'RENDERING.ERROR.RS2_NOT_CONFIGURED',
            };
        }
        const rootUrl = await this.prepareRootUrl();
        const token = securedNode.jwt;
        const request = {
            nodeId: node.ref.id,
            repoId: node.ref.repo,
            securedNode: securedNode.signedNode,
            signature: securedNode.signature,
            signatureAlgorithm: securedNode.signatureAlgorithm,
            token: token,
            renderingBaseUrl:
                rootUrl === securedNode.renderingBaseUrl ? null : securedNode.renderingBaseUrl,
        } as RenderDataRequestWithToken;

        return {
            node,
            nodeParent,
            request,
        };
    }

    async getRenderDataForLms(
        encodedNode: string,
        signature: string,
        jwt: string,
        renderUrl: string,
        signatureAlgorithm: string,
    ): Promise<CombinedRenderData> {
        console.log('Fetching render data for LMS with signature algorithm:', signatureAlgorithm);
        this.injector.get(RSApiConfiguration).rootUrl = renderUrl;
        const decodedNodeString = this.base64ToUtf8(encodedNode);
        const node = JSON.parse(decodedNodeString) as Node;
        const request = {
            nodeId: node.ref.id,
            repoId: node.ref.repo,
            securedNode: encodedNode,
            signature: signature,
            token: jwt,
            signatureAlgorithm: signatureAlgorithm,
        } as RenderDataRequestWithToken;

        return {
            node,
            request,
        };
    }

    async prepareRootUrl() {
        const about = await firstValueFrom(this.aboutService.getAbout());
        const rootUrl = about.renderingService2.url.replace(/\/$/g, '');
        if (this.configuration.production) {
            this.injector.get(RSApiConfiguration).rootUrl = rootUrl;
        } else {
            console.info('dev mode active, routing rendering to proxy');
            this.injector.get(RSApiConfiguration).rootUrl = '/rendering2';
        }
        console.info(this.injector.get(RSApiConfiguration));
        return rootUrl;
    }

    private base64ToUtf8(b64: string): string {
        // Support Base64URL and missing padding
        const normalized = b64.replace(/-/g, '+').replace(/_/g, '/');
        const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);

        const binary = atob(padded);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);

        return new TextDecoder('utf-8', { fatal: false }).decode(bytes);
    }

    private async inheritProps(node: Node) {
        try {
            const parent = await firstValueFrom(
                this.nodeService.getNode(node.parent.id, { repository: node.parent.repo }),
            );
            const config = await firstValueFrom(
                this.configService.observeBackendConfig({ forceUpdate: false }),
            );
            Object.entries(parent.properties).forEach(([k, v]) => {
                // @TODO: This might should be at a better location, i.e. in the widget definition of mds?
                if (config.repository?.childobjects?.ignoredInheritMetadata?.includes(k)) {
                    return;
                }
                if (!node.properties[k]) {
                    node.properties[k] = v;
                }
            });
            return parent;
        } catch (e) {
            e.preventDefault();
        }
        return null;
    }
}
