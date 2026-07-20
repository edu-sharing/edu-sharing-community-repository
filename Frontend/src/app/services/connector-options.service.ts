import { inject, Injectable } from '@angular/core';
import { Connector, ConnectorService } from 'ngx-edu-sharing-api';
import { DefaultGroups, ElementType, OptionItem, UIService } from 'ngx-edu-sharing-ui';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

/**
 * Builds the create-with-connector option list used by the global create menu and the
 * editorial nodes-selector upload tab.
 */
@Injectable({ providedIn: 'root' })
export class ConnectorOptionsService {
    private connectorApi = inject(ConnectorService);
    private ui = inject(UIService);

    /**
     * Observe the filtered list of regular + simple connectors.
     *
     * @param allowedConnectorIds optional whitelist — if set and non-empty, only connectors
     *        whose id is contained are returned.
     */
    observeConnectors(allowedConnectorIds?: string[]): Observable<Connector[]> {
        return this.connectorApi.observeConnectorList().pipe(
            map((list) => {
                const connectors = this.filterConnectors(list?.connectors).concat(
                    this.filterConnectors(list?.simpleConnectors),
                );
                return allowedConnectorIds?.length
                    ? connectors.filter((c) => allowedConnectorIds.includes(c.id))
                    : connectors;
            }),
        );
    }

    /**
     * Observe the OptionItem[] for the create dropdown.
     *
     * @param onSelect called with the picked connector
     * @param allowedConnectorIds optional whitelist of connector ids to offer
     */
    buildOptions(
        onSelect: (connector: Connector) => void,
        allowedConnectorIds?: string[],
    ): Observable<OptionItem[]> {
        return this.observeConnectors(allowedConnectorIds).pipe(
            map((connectors) =>
                connectors.map((connector, i) => {
                    const option = new OptionItem(
                        'CONNECTOR.' + connector.id + '.NAME',
                        connector.icon,
                        () => onSelect(connector),
                    );
                    option.elementType = [ElementType.NoneOrUnknown];
                    option.group = DefaultGroups.CreateConnector;
                    option.priority = i;
                    return option;
                }),
            ),
        );
    }

    private filterConnectors(connectors?: Connector[]): Connector[] {
        return connectors?.filter((c) => !c.onlyDesktop || !this.ui.isMobile()) ?? [];
    }
}
