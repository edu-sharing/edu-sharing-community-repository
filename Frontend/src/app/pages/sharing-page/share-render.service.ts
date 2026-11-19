import { Injectable, inject } from '@angular/core';
import { CombinedRenderData, RenderHelperService } from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { RestSharingService } from '../../core-module/core.module';
import { RenderDataLoader } from '../render2-page/render-wrapper-component/render-data-loader';
import { ShareAccessService } from './share-mds.service';

/**
 * Loads the data to render the shared node or - for a shared folder - one of its elements.
 *
 * Provided by the sharing page, so every render wrapper below it (also the one in the preview of the
 * editorial sidebar) loads through the share instead of the logged in api.
 */
@Injectable()
export class ShareRenderService {
    private access = inject(ShareAccessService);
    private sharingService = inject(RestSharingService);
    private renderHelper = inject(RenderHelperService);

    readonly load: RenderDataLoader = async (nodeId: string): Promise<CombinedRenderData> => {
        try {
            const securedNode = await firstValueFrom(
                this.sharingService.getSecuredNode(
                    this.access.nodeId,
                    this.access.token,
                    this.access.password,
                    nodeId === this.access.nodeId ? null : nodeId,
                ),
            );
            return await this.renderHelper.getRenderDataForShare(securedNode);
        } catch (error) {
            console.warn(error);
            return { node: null, error: 'SHARING.ERROR_RENDER' };
        }
    };
}
