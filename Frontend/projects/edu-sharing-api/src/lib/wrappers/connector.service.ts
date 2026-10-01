import { inject, Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { Connector } from '../api/models/connector';
import { ConnectorFileType } from '../api/models/connector-file-type';
import { ConnectorList } from '../api/models/connector-list';
import { Node } from '../api/models/node';
import { ConnectorV1Service } from '../api/services';
import { HOME_REPOSITORY } from '../constants';
import { RestConstants } from '../rest-constants';
import { shareReplayReturnValue } from '../utils/decorators/share-replay-return-value';
import { switchReplay } from '../utils/rxjs-operators/switch-replay';
import { AuthenticationService } from './authentication.service';

@Injectable({
    providedIn: 'root',
})
export class ConnectorService {
    public static readonly ID_ONLY_OFFICE = 'ONLY_OFFICE';
    public static readonly ID_TINYMCE = 'TINYMCE';

    private authentication = inject(AuthenticationService);
    private connectorV1 = inject(ConnectorV1Service);

    @shareReplayReturnValue()
    observeConnectorList({ repository = HOME_REPOSITORY } = {}): Observable<ConnectorList | null> {
        return this.authentication.observeLoginInfo().pipe(
            map(({ toolPermissions }) => toolPermissions),
            distinctUntilChanged(),
            switchReplay((isValidLogin) => {
                if (isValidLogin) {
                    // TODO: consider also caching this call, so we don't need to send it again
                    // after logout, login.
                    return this.connectorV1.listConnectors({ repository });
                } else {
                    return rxjs.of(null);
                }
            }),
        );
    }

    /**
     * All connectors (regular and simple) at least one of the given nodes belongs to.
     * Each connector is returned at most once.
     */
    observeConnectorsOfNodes(nodes: Node[]): Observable<Connector[]> {
        return this.observeConnectorList().pipe(
            map((list) =>
                [...(list?.connectors ?? []), ...(list?.simpleConnectors ?? [])].filter(
                    (connector) =>
                        nodes?.some((node) =>
                            ConnectorService.nodeBelongsToConnector(node, connector),
                        ),
                ),
            ),
        );
    }

    /**
     * Whether the node was created with the given connector.
     *
     * Elements created by a simple connector carry its id as the `ccm:ccresourcesubtype`,
     * all others are matched via the filetypes the connector declares.
     */
    private static nodeBelongsToConnector(node: Node, connector: Connector): boolean {
        const properties = node?.properties ?? {};
        if (properties[RestConstants.CCM_PROP_CCRESSOURCETYPE]?.[0] === 'connector') {
            return properties[RestConstants.CCM_PROP_CCRESSOURCESUBTYPE]?.[0] === connector.id;
        }
        return !!connector.filetypes?.some((filetype) =>
            ConnectorService.nodeMatchesFiletype(node, filetype),
        );
    }

    /**
     * Whether the node fulfills all criteria the filetype declares. A filetype may be declared
     * via any combination of mimetype, editor type and ccressource type/subtype, e.g. a simple
     * connector might only declare an editor type. A filetype without any criteria matches nothing.
     */
    private static nodeMatchesFiletype(node: Node, filetype: ConnectorFileType): boolean {
        const properties = node?.properties ?? {};
        const criteria: [string | undefined, string | undefined][] = [
            [filetype.mimetype, node?.mimetype],
            [filetype.editorType, properties[RestConstants.CCM_PROP_EDITOR_TYPE]?.[0]],
            [filetype.ccressourcetype, properties[RestConstants.CCM_PROP_CCRESSOURCETYPE]?.[0]],
            [
                filetype.ccresourcesubtype,
                properties[RestConstants.CCM_PROP_CCRESSOURCESUBTYPE]?.[0],
            ],
            [
                filetype.ccressourceversion,
                properties[RestConstants.CCM_PROP_CCRESSOURCEVERSION]?.[0],
            ],
        ].filter(([declared]) => !!declared) as [string, string | undefined][];
        return criteria.length > 0 && criteria.every(([declared, actual]) => declared === actual);
    }
}
