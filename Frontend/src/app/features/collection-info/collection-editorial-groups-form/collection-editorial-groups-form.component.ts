import { Component, inject, input, model, viewChild } from '@angular/core';
import {
    Ace,
    Acl,
    AuthenticationService,
    Group,
    HOME_REPOSITORY,
    IamV1Service,
} from 'ngx-edu-sharing-api';
import {
    ColumnType,
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { RestConstants, RestHelper } from '../../../core-module/core.module';
import { SharedModule } from '../../../shared/shared.module';

/**
 * Lets the user pick the editorial groups that are responsible for an editorial collection, and
 * whether the collection is visible to everyone. The choice is read back as an access control list.
 */
@Component({
    selector: 'es-collection-editorial-groups-form',
    templateUrl: 'collection-editorial-groups-form.component.html',
    styleUrls: ['collection-editorial-groups-form.component.scss'],
    imports: [SharedModule],
})
export class CollectionEditorialGroupsFormComponent {
    private readonly authenticationService = inject(AuthenticationService);
    private readonly iamApi = inject(IamV1Service);

    /** the permissions the collection has, which decide the groups that are selected at first */
    readonly initialPermissions = input<Ace[]>([]);
    /** whether the user may make a collection visible to everyone */
    readonly shareToAll = input(true);
    readonly publicVisible = model(true);

    protected readonly InteractionType = InteractionType;
    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    protected readonly groups = new NodeDataSource<Group>();
    protected readonly columns: ColumnType = {
        Default: [new ListItem('GROUP', RestConstants.AUTHORITY_DISPLAYNAME)],
    };
    private readonly organizations = viewChild<NodeEntriesWrapperComponent<Group>>('organizations');

    constructor() {
        void this.loadGroups();
    }

    /** the permissions that give each selected group the coordination of the collection */
    getAcl(): Acl {
        const acl = { inherited: false, permissions: [] } as Acl;
        if (this.publicVisible()) {
            const everyone = RestHelper.getAllAuthoritiesPermission();
            everyone.permissions = [RestConstants.PERMISSION_CONSUMER];
            acl.permissions.push(everyone);
        }
        for (const group of this.organizations()?.getSelection().selected ?? []) {
            acl.permissions.push({
                authority: {
                    authorityName: group.authorityName,
                    authorityType: group.authorityType,
                },
                permissions: [RestConstants.PERMISSION_COORDINATOR],
            });
        }
        return acl;
    }

    private async loadGroups(): Promise<void> {
        const [search, searchShare] = await Promise.all([
            this.authenticationService.hasToolpermission(
                RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH,
            ),
            this.authenticationService.hasToolpermission(
                RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE,
            ),
        ]);
        if (!search && !searchShare) {
            return;
        }
        this.groups.isLoading = true;
        const result = await firstValueFrom(
            this.iamApi.searchGroups({
                repository: HOME_REPOSITORY,
                pattern: '*',
                global: true,
                groupType: RestConstants.GROUP_TYPE_EDITORIAL,
                maxItems: RestConstants.COUNT_UNLIMITED,
                sortProperties: [RestConstants.CM_PROP_AUTHORITY_DISPLAYNAME],
                sortAscending: [true],
            }),
        );
        this.groups.setData(result.groups as Group[], result.pagination);
        this.groups.isLoading = false;
        // the table only exists once the groups are there
        setTimeout(() => this.selectInitialGroups(result.groups as Group[]));
    }

    private selectInitialGroups(groups: Group[]): void {
        const selected = new Set(
            this.initialPermissions().map((ace) => ace.authority.authorityName),
        );
        this.organizations()
            ?.getSelection()
            .select(...groups.filter((group) => selected.has(group.authorityName)));
    }
}
