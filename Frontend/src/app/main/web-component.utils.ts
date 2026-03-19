import { MockLocationStrategy } from '@angular/common/testing';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { environment } from '../../environments/environment';

/**
 * this location strategy handles navigation behaviour when running as a web component
 * Usually, this means opening things in a new tab if it's impossible to show it in the component
 */
export class WebComponentLocationStrategy extends MockLocationStrategy {
    replaceState(ctx: any, title: string, path: string, query: string) {
        super.replaceState(ctx, title, path, query);
        this.route(path, query);
    }

    pushState(ctx: any, title: string, path: string, query: string) {
        super.pushState(ctx, title, path, query);
        this.route(path, query);
    }
    getBaseUri() {
        return environment.eduSharingApiUrl.substring(
            0,
            environment.eduSharingApiUrl.length - '/rest'.length,
        );
    }
    private route(path: string, query: string) {
        if (
            ['render', 'collections', 'workspace'].some((v) =>
                path.startsWith('/' + UIConstants.ROUTER_PREFIX + v),
            )
        ) {
            window.open(this.getBaseUri() + path + query);
        }
    }
}
