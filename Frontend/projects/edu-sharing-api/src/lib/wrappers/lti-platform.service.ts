import { LtiPlatformV13Service, NodeV1Service, SearchV1Service } from '../api/services';
import { Observable } from 'rxjs';
import { Tools } from '../api/models/tools';
import { Injectable } from '@angular/core';
import { distinctUntilChanged, first, map } from 'rxjs/operators';
import { switchReplay } from '../utils/rxjs-operators/switch-replay';
import * as rxjs from 'rxjs';
import { AuthenticationService } from './authentication.service';
import { shareReplayReturnValue } from '../utils/decorators/share-replay-return-value';
import { Node } from '../api/models/node';
import { RestConstants } from '../rest-constants';

@Injectable({
    providedIn: 'root',
})
export class LtiPlatformService {
    constructor(
        private authentication: AuthenticationService,
        private ltiPlatformService: LtiPlatformV13Service,
    ) {}

    @shareReplayReturnValue()
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

    /**
     * returns an available tool for this node that can edit/open its file type
     * or null if there is none available for this node
     * @param node
     */
    async toolForNode(node: Node) {
        const ltiValue = node.properties?.[RestConstants.CCM_PROP_CCRESSOURCETYPE]?.[0];
        if (!ltiValue) {
            return null;
        }
        const tools = await this.getTools().pipe(first()).toPromise();
        return tools?.tools?.filter((t) => t.resourceType)?.[0];
    }

    convertToLtiResourceLink(nodeId: string, appId: string): Observable<null> {
        return this.ltiPlatformService.convertToResourcelink({ nodeId, appId });
    }
}
