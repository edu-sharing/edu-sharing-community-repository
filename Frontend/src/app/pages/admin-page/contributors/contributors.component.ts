import { Component, OnInit, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ContributorData, ContributorV1Service, HOME_REPOSITORY } from 'ngx-edu-sharing-api';
import { Toast } from '../../../services/toast';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { YES_OR_NO } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { ContributorRegistryEditDialogResult } from '../../../features/dialogs/dialog-modules/contributor-registry-edit-dialog/contributor-registry-edit-dialog-data';

/**
 * Admin tool to manage the autonomous contributor registry (edu_contributor).
 * Gated by TOOLPERMISSION_MANAGE_CONTRIBUTORS (tab visibility in admin-page.component).
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

    contributors: ContributorData[] = [];
    searchWord = '';
    loading = false;

    ngOnInit(): void {
        void this.load();
    }

    async load(): Promise<void> {
        this.loading = true;
        try {
            this.contributors = await firstValueFrom(
                this.contributorService.getContributors({
                    repository: HOME_REPOSITORY,
                    searchWord: this.searchWord || undefined,
                    limit: 200,
                }),
            );
        } catch (e) {
            this.toast.error(e);
        } finally {
            this.loading = false;
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
