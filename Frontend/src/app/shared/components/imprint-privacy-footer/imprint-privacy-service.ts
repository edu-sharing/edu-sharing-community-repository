import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { ConfigService, ConfigV1Service, Context, Node, RestConstants } from 'ngx-edu-sharing-api';
import { OPEN_URL_MODE } from 'ngx-edu-sharing-ui';
import { BridgeService } from 'src/app/services/bridge.service';

/**
 * Id of the config context the node was created in, or null for nodes outside any explicit context.
 */
function getContextId(node: Node | null | undefined): string | null {
    const contextId = node?.properties?.[RestConstants.CCM_PROP_EDUCONTEXT_NAME]?.[0];
    return contextId && contextId !== RestConstants.EDUCONTEXT_DEFAULT ? contextId : null;
}

/**
 * Provides the imprint and privacy links. The imprint follows the config context the node shown by
 * the current page was created in, falling back to the context of the requesting domain.
 */
@Injectable({
    providedIn: 'root',
})
export class ImprintPrivacyService {
    private configService = inject(ConfigService);
    private configV1 = inject(ConfigV1Service);
    private bridge = inject(BridgeService);

    private readonly config = toSignal(this.configService.observeConfig());
    // the node shown by the current page
    private readonly contextNode = signal<Node | null>(null);
    // cancels the reset scheduled for when the page that set the current node is destroyed
    private cancelPendingReset: (() => void) | null = null;
    // loaded on first use only, so pages without a node never request the context list
    private readonly contexts = signal<Context[] | null>(null);
    private contextsRequested = false;

    readonly privacyInformationUrl = computed(() => this.config()?.privacyInformationUrl);
    readonly imprintUrl = computed(() => {
        const domainUrl = this.config()?.imprintUrl;
        const contextId = getContextId(this.contextNode());
        const context = contextId ? this.contexts()?.find((c) => c.id === contextId) : undefined;
        return context?.values?.imprintUrl || domainUrl;
    });

    /**
     * Sets the node that decides the imprint until the page of `destroyRef` is destroyed or another
     * page sets its node. The reset hangs on `destroyRef`, so pages need no unregister call of
     * their own and a node arriving after its page is gone is ignored.
     */
    setContextNode(node: Node | null, destroyRef: DestroyRef) {
        if (destroyRef.destroyed) {
            return;
        }
        // make sure there is only ever one pending reset
        this.cancelPendingReset?.();
        this.cancelPendingReset = destroyRef.onDestroy(() => {
            this.contextNode.set(null);
            this.cancelPendingReset = null;
        });
        this.contextNode.set(node);
        if (getContextId(node)) {
            this.loadContexts();
        }
    }

    openImprint() {
        UIHelper.openUrl(this.imprintUrl(), this.bridge, OPEN_URL_MODE.BlankSystemBrowser);
    }

    openPrivacy() {
        UIHelper.openUrl(
            this.privacyInformationUrl(),
            this.bridge,
            OPEN_URL_MODE.BlankSystemBrowser,
        );
    }

    private loadContexts() {
        if (this.contextsRequested) {
            return;
        }
        this.contextsRequested = true;
        this.configV1.getAvailableContext({ includeStatic: true }).subscribe({
            // the backend lists a null entry while its context cache is empty
            next: (contexts) => this.contexts.set(contexts.filter((c) => !!c)),
            error: () => this.contexts.set([]),
        });
    }
}
