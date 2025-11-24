import { Injectable, Injector, Optional } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { RenderDataRequestWithToken, RSApiConfiguration } from 'ngx-rendering-service-api';
import {
    AboutService,
    HOME_REPOSITORY,
    Node,
    NodeServiceUnwrapped,
    RestConstants,
    UserService,
} from 'ngx-edu-sharing-api';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';
import { OptionsHelperDataService } from './options-helper-data.service';
import { Scope } from '../types/option-item';

export type CombinedRenderData = {
    node: Node;
    request?: RenderDataRequestWithToken;
    error?: string;
};
@Injectable({ providedIn: 'root' })
export class RenderHelperService {
    constructor(
        private injector: Injector,
        private aboutService: AboutService,
        private nodeApiUnwrapped: NodeServiceUnwrapped,
        private configuration: EduSharingUiConfiguration,
        private userService: UserService,
        @Optional() private optionsHelperDataService: OptionsHelperDataService,
    ) {}

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
        const node = securedNode.node;
        const user = await firstValueFrom(this.userService.observeCurrentUserInfo());
        console.info(this.injector.get(OptionsHelperDataService));
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
        console.info(about.renderingService2?.url);
        this.prepareRootUrl();
        console.info(this.injector.get(RSApiConfiguration));
        const token = securedNode.jwt;
        console.info(token, node);
        const request = {
            nodeId: node.ref.id,
            repoId: node.ref.repo,
            securedNode: securedNode.signedNode,
            signature: securedNode.signature,
            token: token,
            userData: {
                authorityName: user.user.person.authorityName,
                firstName: user.user.person.profile.firstName,
                surName: user.user.person.profile.lastName,
                userEMail: user.user.person.profile.email,
            },
        } as RenderDataRequestWithToken;

        return {
            node,
            request,
        };
    }

    async getRenderDataForLms(
        encodedNode: string,
        signature: string,
        jwt: string,
        renderUrl: string,
        encodedUser: string,
    ): Promise<CombinedRenderData> {
        this.injector.get(RSApiConfiguration).rootUrl = renderUrl;
        const decodedNodeString = atob(encodedNode);
        const node = JSON.parse(decodedNodeString) as Node;
        const userData = JSON.parse(atob(encodedUser));
        const request = {
            nodeId: node.ref.id,
            repoId: node.ref.repo,
            securedNode: encodedNode,
            signature: signature,
            token: jwt,
            userData: {
                authorityName: userData.authorityName ?? '',
                firstName: userData.firstName ?? '',
                surName: userData.lastName ?? '',
                userEMail: userData.email ?? '',
            },
        } as RenderDataRequestWithToken;

        return {
            node,
            request,
        };
    }

    async prepareRootUrl() {
        const about = await firstValueFrom(this.aboutService.getAbout());
        if (this.configuration.production) {
            this.injector.get(RSApiConfiguration).rootUrl = about.renderingService2.url.replace(
                /\/$/g,
                '',
            );
        } else {
            console.info('dev mode active, routing rendering to proxy');
            this.injector.get(RSApiConfiguration).rootUrl = '/rendering2';
        }
        console.info(this.injector.get(RSApiConfiguration));
    }
}
