import { LtiPlatformV13Service, NodeV1Service, SearchV1Service } from '../api/services';
import { Observable } from 'rxjs';
import { Tools } from '../api/models/tools';
import { Injectable } from '@angular/core';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { switchReplay } from '../utils/rxjs-operators/switch-replay';
import * as rxjs from 'rxjs';
import { AuthenticationService } from './authentication.service';

@Injectable({
    providedIn: 'root',
})
export class LtiPlatformService {
    constructor(
        private authentication: AuthenticationService,
        private ltiPlatformService: LtiPlatformV13Service,
    ) {}

    getTools(): Observable<Tools | null> {
        return this.authentication.observeLoginInfo().pipe(
            map(({ isValidLogin }) => isValidLogin),
            distinctUntilChanged(),
            switchReplay((isValidLogin) => {
                if (isValidLogin) {
                    // TODO: consider also caching this call, so we don't need to send it again
                    // after logout, login.
                    return this.ltiPlatformService.tools();
                } else {
                    return rxjs.of(null);
                }
            }),
        );
    }

    convertToLtiResourceLink(nodeId: string, appId: string): Observable<null> {
        return this.ltiPlatformService.convertToResourcelink({ nodeId, appId });
    }
}
