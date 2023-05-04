import { trigger } from '@angular/animations';
import { Component, Inject, OnInit } from '@angular/core';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { BehaviorSubject, Observable } from 'rxjs';
import { debounceTime, filter, startWith, switchMap } from 'rxjs/operators';
import {
    DialogButton,
    RestConstants,
    RestIamService,
    RestSearchService,
    VCardResult,
} from '../../../../core-module/core.module';
import { DateHelper, UIAnimation, VCard } from 'ngx-edu-sharing-ui';
import { Toast } from '../../../../core-ui-module/toast';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    ContributorEditDialogData,
    ContributorEditDialogResult,
} from './contributor-edit-dialog-data';

// TODO: Use immutable data for models, so we can (reliably) set the dialog's `closable` config more
// sensibly.

@Component({
    selector: 'es-contributor-edit-dialog',
    templateUrl: './contributor-edit-dialog.component.html',
    styleUrls: ['./contributor-edit-dialog.component.scss'],
    animations: [trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
})
export class ContributorEditDialogComponent implements OnInit {
    readonly roles = this.getAvailableRoles();
    vCard: VCard;
    editType: number | string; // FIXME: weird type
    editDisabled = false;
    userAuthor = false;
    role: string;
    date: Date;
    showPersistentIds = false;
    more = false;
    private fullName = new BehaviorSubject('');
    private orgName = new BehaviorSubject('');
    suggestionPersons$: Observable<VCardResult[]>;
    suggestionOrgs$: Observable<VCardResult[]>;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: ContributorEditDialogData,
        private dialogRef: CardDialogRef<ContributorEditDialogData, ContributorEditDialogResult>,
        private searchService: RestSearchService,
        private iamService: RestIamService,
        private toast: Toast,
    ) {}

    ngOnInit(): void {
        this.initObservables();
        this.initButtons();
        this.initData();
        this.updateSubtitle();
    }

    updateSubtitle() {
        this.dialogRef.patchConfig({
            subtitle: 'WORKSPACE.CONTRIBUTOR.TYPE.' + this.role.toUpperCase(),
        });
    }

    private initData() {
        if (this.data.vCard) {
            this.role = this.data.role;
            void this.initWithVCard(this.data.vCard);
        } else {
            this.role = this.getAvailableRoles()[0];
            this.resetVCard();
        }
    }

    private getAvailableRoles(): string[] {
        switch (this.data.editMode) {
            case 'lifecycle':
                return RestConstants.CONTRIBUTOR_ROLES_LIFECYCLE;
            case 'metadata':
                return RestConstants.CONTRIBUTOR_ROLES_METADATA;
        }
    }

    private initButtons() {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
                new DialogButton('APPLY', { color: 'primary' }, () => this.applyChanges()),
            ],
        });
    }

    private initObservables() {
        this.suggestionPersons$ = this.fullName.pipe(
            startWith(''),
            filter((v) => v !== ''),
            debounceTime(200),
            switchMap((v) => this.searchService.searchContributors(v.trim(), 'PERSON')),
        );
        this.suggestionOrgs$ = this.orgName.pipe(
            startWith(''),
            filter((v) => v !== ''),
            debounceTime(200),
            switchMap((v) => this.searchService.searchContributors(v.trim(), 'ORGANIZATION')),
        );
    }

    private async initWithVCard(vCard: VCard) {
        this.vCard = vCard;
        this.editDisabled = !!(vCard.orcid || vCard.gnduri || vCard.ror || vCard.wikidata);
        this.editType = vCard.getType();
        if (vCard.uid === (await this.iamService.getCurrentUserVCard()).uid) {
            this.userAuthor = true;
            this.editDisabled = true;
        }
        if (vCard.contributeDate) {
            this.date = new Date(vCard.contributeDate);
        } else {
            this.date = null;
        }
    }

    resetVCard() {
        this.date = null;
        this.vCard = new VCard();
        this.editType = this.vCard.getType();
        this.editDisabled = false;
    }

    openDatepicker() {
        this.date = new Date();
    }

    async setVCardAuthor(set: boolean) {
        if (set) {
            this.vCard = await this.iamService.getCurrentUserVCard();
            this.userAuthor = true;
            this.editDisabled = true;
        } else {
            this.resetVCard();
        }
    }

    useVCardSuggestion(event: MatAutocompleteSelectedEvent) {
        this.vCard = event.option.value.copy();
        this.editDisabled = true;
    }

    private applyChanges() {
        // TODO: Use form validators instead of this.
        if (this.editType == VCard.TYPE_PERSON && (!this.vCard.givenname || !this.vCard.surname)) {
            this.toast.error(null, 'WORKSPACE.CONTRIBUTOR.ERROR_PERSON_NAME');
            return;
        }
        if (this.editType == VCard.TYPE_ORG && !this.vCard.org) {
            this.toast.error(null, 'WORKSPACE.CONTRIBUTOR.ERROR_ORG_NAME');
            return;
        }
        if (this.editType == VCard.TYPE_ORG) {
            this.vCard.givenname = '';
            this.vCard.surname = '';
            this.vCard.title = '';
        }
        this.vCard.contributeDate = this.date
            ? DateHelper.getDateFromDatepicker(this.date).toISOString()
            : null;
        this.dialogRef.close({
            vCard: this.vCard,
            role: this.role,
        });
    }

    setFullName() {
        this.fullName.next(this.vCard.givenname + ' ' + this.vCard.surname);
    }

    setOrgName() {
        this.orgName.next(this.vCard.org);
    }
}
