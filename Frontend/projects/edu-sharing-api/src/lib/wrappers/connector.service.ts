import { Injectable, inject } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { ConnectorList } from '../api/models/connector-list';
import { ConnectorV1Service } from '../api/services';
import { HOME_REPOSITORY } from '../constants';
import { shareReplayReturnValue } from '../utils/decorators/share-replay-return-value';
import { switchReplay } from '../utils/rxjs-operators/switch-replay';
import { AuthenticationService } from './authentication.service';

@Injectable({
    providedIn: 'root',
})
export class ConnectorService {
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
}
