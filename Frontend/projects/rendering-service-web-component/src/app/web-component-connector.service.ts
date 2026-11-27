import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import {
    Connector,
    ConnectorList,
    ConnectorService,
    HOME_REPOSITORY,
    Node,
} from 'ngx-edu-sharing-api';
import { UIService } from 'ngx-edu-sharing-ui';
import { RenderingConnectorService } from 'ngx-rendering-service-lib';
import { firstValueFrom } from 'rxjs';
import { environment } from '../environments/environment';

/**
 * Connector support of the rendering web component (e.g. to edit an office document embedded in a
 * lms with ONLYOFFICE).
 *
 * The web component has no repository session, so it can neither check the tool permissions nor
 * the write permission of the user. It only offers opening if a connector supports the material.
 * The repository's connector servlet is opened in a new window: it requires a login (redirecting to
 * the login if necessary) and then decides about the permissions (read only, no access, ...).
 *
 * The connector list is loaded with the rendering token (see render-token.interceptor), the
 * repository accepts it for this endpoint.
 */
@Injectable({ providedIn: 'root' })
export class WebComponentConnectorService extends RenderingConnectorService {
    private http = inject(HttpClient);
    private ui = inject(UIService);
    private connectorList: Promise<ConnectorList | null> | null = null;

    async hasAvailableConnector(node: Node): Promise<boolean> {
        return (await this.getConnector(node)) != null;
    }

    async openConnector(node: Node): Promise<void> {
        // open the window right away (still in the user's click) to not be blocked as popup
        const win = window.open('', '_blank');
        const connector = await this.getConnector(node);
        if (!connector?.id) {
            win?.close();
            return;
        }
        const url = ConnectorService.getConnectorUrl(
            environment.eduSharingApiUrl,
            connector.id,
            node.ref.id,
        );
        if (win) {
            // the connector must not be able to access the embedding page
            win.opener = null;
            win.location.href = url;
        } else {
            window.open(url, '_blank', 'noopener');
        }
    }

    /** the connector to open the node with (same matching as the app, without permissions) */
    private async getConnector(node: Node): Promise<Connector | null> {
        // files of the home repository only, like the "open" option of the app
        if (!node?.ref?.id || node.isDirectory || node.ref.isHomeRepo === false) {
            return null;
        }
        const list = await this.getConnectorList();
        const connectors = [
            ...(list?.connectors ?? []),
            // simple connectors which declare mimetypes can be matched like regular ones
            ...(list?.simpleConnectors ?? []).filter((connector) =>
                connector.filetypes?.some((filetype) => !!filetype.mimetype),
            ),
        ].filter((connector) => !connector.onlyDesktop || !this.ui.isMobile());
        return (
            connectors.find((connector) => ConnectorService.getFiletype(node, connector) != null) ??
            null
        );
    }

    /** loaded once per page (it is configuration only), a failed request is retried next time */
    private getConnectorList(): Promise<ConnectorList | null> {
        if (!this.connectorList) {
            this.connectorList = firstValueFrom(
                this.http.get<ConnectorList>(
                    `${environment.eduSharingApiUrl}/connector/v1/connectors/${HOME_REPOSITORY}/list`,
                ),
            ).catch((e): null => {
                console.warn('could not load the connectors', e);
                this.connectorList = null;
                return null;
            });
        }
        return this.connectorList;
    }
}
