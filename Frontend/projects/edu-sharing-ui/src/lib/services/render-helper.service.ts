import { Injectable, Injector, Optional } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { RenderDataRequestWithToken, RSApiConfiguration } from 'ngx-rendering-service-api';
import {
    AboutService,
    NodeService,
    NodeServiceUnwrapped,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';
import { OptionsHelperDataService } from './options-helper-data.service';
import { Scope } from '../types/option-item';

@Injectable({
    providedIn: 'root',
})
export class RenderHelperService {
    constructor(
        private injector: Injector,
        private aboutService: AboutService,
        private nodeApiUnwrapped: NodeServiceUnwrapped,
        private nodeApi: NodeService,
        private configuration: EduSharingUiConfiguration,
        @Optional() private optionsHelperDataService: OptionsHelperDataService,
    ) {}

    async getRenderData(nodeId: string, version: string = null) {
        const about = await firstValueFrom(this.aboutService.getAbout());
        if (!about.renderingService2) {
            console.error('no rendering service 2 url was configured. Will not continue.');
            return null;
        }
        console.log(about.renderingService2?.url);
        if (this.configuration.production) {
            this.injector.get(RSApiConfiguration).rootUrl = about.renderingService2.url.replace(
                /\/$/g,
                '',
            );
        } else {
            this.injector.get(RSApiConfiguration).rootUrl = '/rendering2';
        }
        console.log(this.injector.get(RSApiConfiguration));
        const node = await firstValueFrom(this.nodeApi.getNode(nodeId));
        const token = await (
            (await firstValueFrom(
                this.nodeApiUnwrapped.getJwt({
                    repository: node.ref.repo,
                    node: node.ref.id,
                }),
            )) as unknown as Blob
        ).text();
        //const token = 'tst';
        console.log(token, node);
        const resourceType =
            node.properties[RestConstants.CCM_PROP_CCRESSOURCETYPE] === undefined
                ? ''
                : node.properties[RestConstants.CCM_PROP_CCRESSOURCETYPE][0] ?? '';
        const request = {
            nodeId: node.ref.id,
            size: parseInt(node.size),
            hash: node.content.hash,
            mimeType: node.mimetype ?? '',
            type: node.mediatype,
            repoId: node.ref.repo,
            version: node.content.version,
            resourceType: resourceType,
            url: node.properties?.['ccm:wwwurl']?.[0] || '',
            // the replication source flag can be set in order to trigger special treatments
            // in the backend. For example, it can be used for sodix paid media in order to
            // fetch two instead of one url. This logic has to be implemented
            replicationSourceFlag: false,
            token: token,
        } as RenderDataRequestWithToken;
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
        return {
            node,
            request,
        };
    }
}
