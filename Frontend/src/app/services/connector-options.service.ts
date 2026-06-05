import { Injectable, inject } from '@angular/core';
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

    /** Observe the filtered list of regular + simple connectors. */
    observeConnectors(): Observable<Connector[]> {
        return this.connectorApi
            .observeConnectorList()
            .pipe(
                map((list) =>
                    this.filterConnectors(list?.connectors).concat(
                        this.filterConnectors(list?.simpleConnectors),
                    ),
                ),
            );
    }

    /** Observe the OptionItem[] for the create dropdown. */
    buildOptions(onSelect: (connector: Connector) => void): Observable<OptionItem[]> {
        return this.observeConnectors().pipe(
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
