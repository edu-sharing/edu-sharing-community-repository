import { Component, OnInit, inject } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { firstValueFrom } from 'rxjs';
import { ContributorData, ContributorV1Service, HOME_REPOSITORY } from 'ngx-edu-sharing-api';
import { Toast } from '../../../services/toast';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { YES_OR_NO } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { ContributorRegistryEditDialogResult } from '../../../features/dialogs/dialog-modules/contributor-registry-edit-dialog/contributor-registry-edit-dialog-data';

type ContributorKind = 'PERSON' | 'ORGANIZATION';
type ContributorSortBy = 'NAME' | 'KIND' | 'CREATED' | 'LAST_UPDATED' | 'IDS';
type ContributorIdType = 'ORCID' | 'GND' | 'ROR' | 'WIKIDATA' | 'EMAIL';

/**
 * Admin tool to manage the autonomous contributor registry (edu_contributor).
 * Gated by TOOLPERMISSION_MANAGE_CONTRIBUTORS (tab visibility in admin-page.component).
 * Filtering, sorting, paging and the total match count are resolved server-side via
 * ContributorV1Service.listContributors (GET /contributor/v1/{repository}/list).
 */
@Component({
    selector: 'es-admin-contributors',
    templateUrl: 'contributors.component.html',
    styleUrls: ['contributors.component.scss'],
    standalone: false,
})
export class AdminContributorsComponent implements OnInit {
    private contributorService = inject(ContributorV1Service);
    private toast = inject(Toast);
    private dialogs = inject(DialogsService);

    readonly displayedColumns = ['name', 'kind', 'ids', 'lastUpdated', 'created', 'actions'];
    /**
     * Persistent id types per kind: organizations carry ROR / Wikidata, persons ORCID / GND,
     * an e-mail can belong to both. The id filter options are scoped to the selected kind.
     */
    readonly idTypes: { value: ContributorIdType; label: string; kinds: ContributorKind[] }[] = [
        { value: 'ORCID', label: 'ORCID', kinds: ['PERSON'] },
        { value: 'GND', label: 'GND', kinds: ['PERSON'] },
        { value: 'ROR', label: 'ROR', kinds: ['ORGANIZATION'] },
        { value: 'WIKIDATA', label: 'Wikidata', kinds: ['ORGANIZATION'] },
        { value: 'EMAIL', label: 'E-Mail', kinds: ['PERSON', 'ORGANIZATION'] },
    ];
    readonly pageSizeOptions = [10, 25, 50, 100];

    contributors: ContributorData[] = [];
    total = 0;

    searchWord = '';
    kind?: ContributorKind;
    hasId: ContributorIdType[] = [];
    sortBy: ContributorSortBy = 'NAME';
    sortAscending = true;
    skip = 0;
    pageIndex = 0;
    pageSize = 25;
    loading = false;

    ngOnInit(): void {
        void this.load();
    }

    async load(): Promise<void> {
        this.loading = true;
        try {
            const result = await firstValueFrom(
                this.contributorService.listContributors({
                    repository: HOME_REPOSITORY,
                    searchWord: this.searchWord || undefined,
                    kind: this.kind,
                    hasId: this.hasId.length ? this.hasId : undefined,
                    sortBy: this.sortBy,
                    sortAscending: this.sortAscending,
                    skip: this.skip,
                    limit: this.pageSize,
                }),
            );
            this.contributors = result.contributors;
            this.total = result.pagination.total;
        } catch (e) {
            this.toast.error(e);
        } finally {
            this.loading = false;
        }
    }

    /** id filter options scoped to the currently selected kind (all when no kind is selected) */
    get availableIdTypes(): { value: ContributorIdType; label: string }[] {
        return this.kind ? this.idTypes.filter((t) => t.kinds.includes(this.kind!)) : this.idTypes;
    }

    /** kind change: drop id filters that don't apply to the new kind, then reload */
    onKindChange(): void {
        const allowed = new Set(this.availableIdTypes.map((t) => t.value));
        this.hasId = this.hasId.filter((id) => allowed.has(id));
        this.onFilterChange();
    }

    /** filter / search change: jump back to the first page and reload */
    onFilterChange(): void {
        this.skip = 0;
        this.pageIndex = 0;
        void this.load();
    }

    onSort(sort: Sort): void {
        this.sortBy = this.toSortBy(sort.active);
        this.sortAscending = sort.direction !== 'desc';
        this.skip = 0;
        this.pageIndex = 0;
        void this.load();
    }

    onPage(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.skip = event.pageIndex * event.pageSize;
        void this.load();
    }

    private toSortBy(active: string): ContributorSortBy {
        switch (active) {
            case 'kind':
                return 'KIND';
            case 'ids':
                return 'IDS';
            case 'created':
                return 'CREATED';
            case 'lastUpdated':
                return 'LAST_UPDATED';
            default:
                return 'NAME';
        }
    }

    async startCreate(): Promise<void> {
        const dialogRef = await this.dialogs.openContributorRegistryEditDialog({});
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                void this.persist(null, result);
            }
        });
    }

    async startEdit(contributor: ContributorData): Promise<void> {
        const dialogRef = await this.dialogs.openContributorRegistryEditDialog({ contributor });
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                void this.persist(contributor.id, result);
            }
        });
    }

    async remove(contributor: ContributorData): Promise<void> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'ADMIN.CONTRIBUTORS.DELETE_TITLE',
            message: 'ADMIN.CONTRIBUTORS.DELETE_MESSAGE',
            messageParameters: { name: this.displayName(contributor) },
            buttons: YES_OR_NO,
        });
        dialogRef.afterClosed().subscribe(async (response) => {
            if (response !== 'YES') {
                return;
            }
            try {
                await firstValueFrom(
                    this.contributorService.deleteContributor({
                        repository: HOME_REPOSITORY,
                        id: contributor.id,
                    }),
                );
                this.toast.toast('ADMIN.CONTRIBUTORS.DELETED');
                await this.load();
            } catch (e) {
                this.toast.error(e);
            }
        });
    }

    displayName(contributor: ContributorData): string {
        const name = [contributor.givenname, contributor.surname]
            .filter((p) => !!p)
            .join(' ')
            .trim();
        return name || contributor.org || contributor.email || '' + contributor.id;
    }

    private async persist(
        id: number | null,
        result: ContributorRegistryEditDialogResult,
    ): Promise<void> {
        const body = this.toBody(result.contributor);
        try {
            if (id == null) {
                await firstValueFrom(
                    this.contributorService.createContributor({
                        repository: HOME_REPOSITORY,
                        body,
                    }),
                );
            } else {
                await firstValueFrom(
                    this.contributorService.updateContributor({
                        repository: HOME_REPOSITORY,
                        id,
                        body: { ...body, applyToExisting: result.applyToExisting },
                    }),
                );
            }
            this.toast.toast('ADMIN.CONTRIBUTORS.SAVED');
            await this.load();
        } catch (e) {
            this.toast.error(e);
        }
    }

    /** picks only the request fields - extra fields (id, vcard, timestamps) would fail backend deserialization */
    private toBody(model: Partial<ContributorData>) {
        return {
            kind: model.kind,
            title: model.title,
            givenname: model.givenname,
            surname: model.surname,
            org: model.org,
            email: model.email,
            url: model.url,
            uid: model.uid,
            orcid: model.orcid,
            gnduri: model.gnduri,
            ror: model.ror,
            wikidata: model.wikidata,
        };
    }
}
