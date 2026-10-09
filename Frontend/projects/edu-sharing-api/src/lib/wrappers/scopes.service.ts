import { Injectable, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BehaviorSubject, Observable, firstValueFrom, of, timer } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap, takeWhile, tap } from 'rxjs/operators';
import { HOME_REPOSITORY } from '../constants';
import { ScopeV1Service } from '../api/services';
import { ActiveScope } from '../api/models/active-scope';
import { ScopeInfo } from '../api/models/scope-info';
import { ScopeField } from '../api/models/scope-field';
import { ScopeBulkRequest } from '../api/models/scope-bulk-request';
import { ScopeBulkStatus } from '../api/models/scope-bulk-status';
import { NodeScope, NodeWithScopes } from '../model-overrides/scopes';
import { Node } from '../api/models/node';
import { AboutService } from './about.service';
import { AuthenticationService } from './authentication.service';

/**
 * Scopes (editorial / customer specific assignments of content).
 *
 * Keeps the active scope of the session (at most one) and wraps the scope api. Every component that
 * depends on the active scope reads it from here, so a switch reaches all of them at once.
 */
@Injectable({
    providedIn: 'root',
})
export class ScopesService {
    private api = inject(ScopeV1Service);
    private about = inject(AboutService);
    private authentication = inject(AuthenticationService);

    /** whether the feature flag `repository.scopes.enabled` is on (`/about` features) */
    readonly enabled$: Observable<boolean> = this.about
        .getAbout()
        .pipe(map((about) => !!about.features?.some((f) => f.id === 'scopes')));
    readonly enabled = toSignal(this.enabled$, { initialValue: false });

    private readonly activeScope$ = new BehaviorSubject<ActiveScope | null>(null);
    /** active scope of the session, null if the user works globally */
    readonly activeScope = toSignal(this.activeScope$, { initialValue: null });
    /** true if the user is a member of the active scope group (may assign content) */
    readonly isEditor = computed(() => this.activeScope()?.role === 'EDITOR');
    /** true if the user is an org admin of the active scope group (may change the base query) */
    readonly isOrgAdmin = computed(() => !!this.activeScope()?.orgAdmin);
    /** field list of the active scope */
    readonly fields = computed<ScopeField[]>(() => this.activeScope()?.definition?.fields ?? []);

    constructor() {
        // the active scope depends on the session (domain context, selection): reload on user changes
        this.authentication
            .observeLoginInfo()
            .pipe(
                map((login) => login?.isValidLogin + ':' + login?.authorityName),
                distinctUntilChanged(),
                switchMap(() => this.enabled$),
                switchMap((enabled) => (enabled ? this.fetchActiveScope() : of(null))),
            )
            .subscribe((scope) => this.activeScope$.next(scope));
    }

    observeActiveScope(): Observable<ActiveScope | null> {
        return this.activeScope$.asObservable();
    }

    /** scopes offered by the switcher (own scope groups plus the temporary scope of the domain) */
    getAvailableScopes(): Observable<ScopeInfo[]> {
        return this.api.getScopes({ repository: HOME_REPOSITORY });
    }

    /** switch the active scope, null switches to global */
    async setActiveScope(scopeId: string | null): Promise<ActiveScope | null> {
        await firstValueFrom(
            this.api.setCurrentScope({ repository: HOME_REPOSITORY, body: { scopeId } }),
        );
        return this.refresh();
    }

    async refresh(): Promise<ActiveScope | null> {
        const scope = await firstValueFrom(this.fetchActiveScope());
        this.activeScope$.next(scope);
        return scope;
    }

    getField(property: string): ScopeField | undefined {
        return this.fields().find((f) => f.id === property);
    }

    /** entry of the given node in the active scope (from the node dto, no request) */
    getNodeScope(node: Node, scopeId = this.activeScope()?.scopeId): NodeScope | undefined {
        return (node as NodeWithScopes)?.scopes?.find((s) => s.scopeId === scopeId);
    }

    /** published, public scope entries of other scopes, e.g. "highly recommended by WLO" */
    getForeignScopes(node: Node): NodeScope[] {
        const own = this.activeScope()?.scopeId;
        return ((node as NodeWithScopes)?.scopes ?? []).filter(
            (s) => s.scopeId !== own && s.published,
        );
    }

    getAssignment(scopeId: string, node: string) {
        return this.api.getAssignment1({ repository: HOME_REPOSITORY, scopeId, node }).pipe(
            // 404: the node has no entry in this scope yet
            catchError(() => of(null)),
        );
    }

    setAssignment(
        scopeId: string,
        node: string,
        properties: { [key: string]: string[] },
        reason?: string,
    ) {
        return this.api.setAssignment({
            repository: HOME_REPOSITORY,
            scopeId,
            node,
            body: { properties, reason },
        });
    }

    deleteAssignment(scopeId: string, node: string, reason?: string) {
        return this.api.deleteAssignment1({ repository: HOME_REPOSITORY, scopeId, node, reason });
    }

    getHistory(scopeId: string, node: string) {
        return this.api.getHistory({ repository: HOME_REPOSITORY, scopeId, node });
    }

    /** start a bulk job, returns the job id */
    startBulk(scopeId: string, request: ScopeBulkRequest): Observable<string> {
        return this.api
            .startBulk({ repository: HOME_REPOSITORY, scopeId, body: request })
            .pipe(map((jobId) => String(jobId).replace(/^"|"$/g, '')));
    }

    /** polls the job until it is finished or failed (emits every state) */
    observeBulkJob(scopeId: string, jobId: string, intervalMs = 2000): Observable<ScopeBulkStatus> {
        return timer(0, intervalMs).pipe(
            switchMap(() =>
                this.api.getBulkStatus({ repository: HOME_REPOSITORY, scopeId, jobId }),
            ),
            takeWhile((s) => s.state === 'QUEUED' || s.state === 'RUNNING', true),
        );
    }

    setBaseQuery(scopeId: string, baseQuery: ActiveScope['definition']['baseQuery']) {
        return this.api
            .setBaseQuery({ repository: HOME_REPOSITORY, scopeId, body: baseQuery })
            .pipe(tap(() => void this.refresh()));
    }

    // admin

    getAllScopes() {
        return this.api.getAllScopes({ repository: HOME_REPOSITORY });
    }

    createScope(body: Parameters<ScopeV1Service['createScope']>[0]['body']) {
        return this.api.createScope({ repository: HOME_REPOSITORY, body });
    }

    updateScope(scopeId: string, body: Parameters<ScopeV1Service['updateScope']>[0]['body']) {
        return this.api.updateScope({ repository: HOME_REPOSITORY, scopeId, body });
    }

    private fetchActiveScope(): Observable<ActiveScope | null> {
        return this.api.getCurrentScope({ repository: HOME_REPOSITORY }).pipe(
            // 204 (global) has no body
            map((scope) => scope || null),
            catchError(() => of(null)),
        );
    }
}
