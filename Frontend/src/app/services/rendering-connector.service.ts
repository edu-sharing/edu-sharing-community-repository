import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ConnectorService, Node } from 'ngx-edu-sharing-api';
import { RenderingConnectorService as RenderingConnectorServiceAbstract } from 'ngx-rendering-service-lib';
import { UIService } from '../core-module/rest/services/ui.service';

/**
 * App implementation of the rendering lib's connector bridge, mirroring the "Open" option
 * (`editConnectorNode` in `options/view-options.ts`).
 */
@Injectable({ providedIn: 'root' })
export class RenderingConnectorService extends RenderingConnectorServiceAbstract {
    private connectorApi = inject(ConnectorService);
    private uiService = inject(UIService);

    async hasAvailableConnector(node: Node): Promise<boolean> {
        // same constrains as the option: files of the home repository only
        if (!node || node.isDirectory || node.ref?.isHomeRepo === false) {
            return false;
        }
        // the connector list is loaded asynchronously, make sure it is available before matching
        await firstValueFrom(this.connectorApi.observeConnectorList());
        return this.uiService.hasAvailableConnector(node);
    }

    async openConnector(node: Node): Promise<void> {
        await this.uiService.editConnector(node);
    }
}
