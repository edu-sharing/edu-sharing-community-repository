import { inject, Injectable } from '@angular/core';
import { ConfigService, LtiPlatformService, Node, Tool } from 'ngx-edu-sharing-api';
import { DefaultGroups, ElementType, OptionItem } from 'ngx-edu-sharing-ui';
import { firstValueFrom, Observable, of } from 'rxjs';
import { catchError, filter, map, shareReplay, startWith, switchMap } from 'rxjs/operators';
import { RestConstants, RestHelper, RestNodeService } from '../core-module/core.module';
import { UIHelper } from '../core-ui-module/ui-helper';
import { NodeHelperService } from './node-helper.service';

/**
 * The result of the `CreateLtiToolDialogComponent`.
 */
export type LtiToolDialogResult = { nodes?: Node[]; name?: string; window?: Window };

/**
 * Builds the "create via LTI tool" option list and performs the node creation for the picked tool.
 * Used by the global create menu and the editorial nodes-selector upload tab.
 */
@Injectable({ providedIn: 'root' })
export class LtiToolOptionsService {
    private configService = inject(ConfigService);
    private ltiPlatformService = inject(LtiPlatformService);
    private nodeService = inject(RestNodeService);
    private nodeHelper = inject(NodeHelperService);

    private readonly tools$: Observable<Tool[]> = this.configService
        .observeEndpointAllowed('LTI')
        .pipe(
            filter((allowed) => allowed),
            switchMap(() =>
                this.ltiPlatformService.getTools().pipe(
                    catchError((error) => {
                        // ignore errors, the tools are simply not offered then
                        error?.preventDefault?.();
                        return of(null);
                    }),
                ),
            ),
            map((tools) => tools?.tools ?? []),
            // the endpoint may never be allowed, so emit an empty list right away
            startWith([] as Tool[]),
            shareReplay({ bufferSize: 1, refCount: false }),
        );

    /**
     * Observe the LTI tools that can be used to create a new element.
     */
    observeTools(): Observable<Tool[]> {
        return this.tools$;
    }

    /**
     * Observe the OptionItem[] for the create dropdown.
     *
     * @param onSelect called with the picked tool
     */
    buildOptions(onSelect: (tool: Tool) => void): Observable<OptionItem[]> {
        return this.observeTools().pipe(
            map((tools) =>
                tools.map((tool, i) => {
                    const option = new OptionItem(tool.name, 'edit', () => onSelect(tool));
                    option.elementType = [ElementType.NoneOrUnknown];
                    option.group = DefaultGroups.CreateLtiTools;
                    option.priority = i;
                    return option;
                }),
            ),
        );
    }

    /**
     * Creates the node(s) for a confirmed `CreateLtiToolDialogComponent` and returns them.
     *
     * For a `customContentOption` tool the dialog already opened the popup window (within the user
     * gesture) and hands it over via `result.window` — do not open one here, it would be killed by
     * the popup blocker.
     *
     * @param tool the tool the dialog was opened for
     * @param result the dialog result
     * @param resolveParent resolves the folder the node is created in — only called (and awaited)
     *        for `customContentOption` tools
     */
    createFromDialogResult(
        tool: Tool,
        result: LtiToolDialogResult,
        resolveParent: () => Node | Promise<Node>,
    ): Promise<Node[]> {
        if (tool.customContentOption) {
            return this.createContentOptionNode(tool, result.name, resolveParent, result.window);
        }
        return this.renameCreatedNodes(result.nodes ?? []);
    }

    /**
     * Deletes the nodes a cancelled LTI tool dialog had already created.
     */
    cancelDialogResult(result: LtiToolDialogResult): void {
        result?.nodes?.forEach((node) => {
            this.nodeService.deleteNode(node.ref.id, false).subscribe({
                next: () => {},
                error: (error) => this.nodeHelper.handleNodeError(node.name, error),
            });
        });
    }

    /**
     * Creates an empty node for a `customContentOption` tool, converts it into an LTI resource link
     * and opens that link in the pre-opened window.
     */
    private async createContentOptionNode(
        tool: Tool,
        name: string,
        resolveParent: () => Node | Promise<Node>,
        win: Window,
    ): Promise<Node[]> {
        if (name == null) {
            win?.close();
            return [];
        }
        try {
            const parent = await resolveParent();
            const properties = RestHelper.createNameProperty(name);
            const created = await firstValueFrom(
                this.nodeService.createNode(
                    parent.ref.id,
                    RestConstants.CCM_TYPE_IO,
                    [],
                    properties,
                ),
            );
            await firstValueFrom(
                this.ltiPlatformService.convertToLtiResourceLink(created.node.ref.id, tool.appId),
            );
            UIHelper.openLTIResourceLink(win, created.node);
            return [created.node];
        } catch (error) {
            this.nodeHelper.handleNodeError(name, error);
            win?.close();
            return [];
        }
    }

    /**
     * The deep-link flow already created the nodes, only the (editable) name has to be written back.
     */
    private async renameCreatedNodes(nodes: Node[]): Promise<Node[]> {
        const result: Node[] = [];
        for (const node of nodes) {
            try {
                const updated = await firstValueFrom(
                    this.nodeService.editNodeMetadata(
                        node.ref.id,
                        RestHelper.createNameProperty(node.name),
                    ),
                );
                result.push(updated.node);
            } catch (error) {
                this.nodeHelper.handleNodeError(node.name, error);
            }
        }
        return result;
    }
}
