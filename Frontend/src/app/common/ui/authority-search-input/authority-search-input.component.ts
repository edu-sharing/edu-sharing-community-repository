import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete/autocomplete';
import {
    Authority,
    AuthorityProfile,
    Group,
    IamAuthorities,
    RestConstants,
    RestIamService,
    RestOrganizationService,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { NodeHelper } from '../../../core-ui-module/node-helper';
import { PermissionNamePipe } from '../../../core-ui-module/pipes/permission-name.pipe';
import { SuggestItem } from '../autocomplete/autocomplete.component';

@Component({
    selector: 'authority-search-input',
    templateUrl: 'authority-search-input.component.html',
    styleUrls: ['authority-search-input.component.scss'],
})
export class AuthoritySearchInputComponent {
    @Input() globalSearchAllowed = false;
    /**
     * Do allow any entered authority (not recommended for general use)
     */
    @Input() allowAny = false;
    /**
     * Group type to filter the groups searched for
     */
    @Input() groupType = '';
    /**
     * maximum number of authorities to fetch in total
     */
    @Input() authorityCount = 50;
    /**
     * Show recent invited users
     */
    @Input() showRecent = true;
    @Input() mode = AuthoritySearchMode.UsersAndGroups;
    @Input() disabled = false;
    @Input() maxSuggestions = 10;
    @Input() inputIcon = 'search';
    @Input() placeholder = 'WORKSPACE.INVITE_FIELD';
    @Input() hint = '';

    @Output() onChooseAuthority = new EventEmitter();

    inputValue = '';
    suggestionGroups: any;
    affiliation = true;

    constructor(
        private iam: RestIamService,
        private organization: RestOrganizationService,
        private namePipe: PermissionNamePipe,
    ) {}

    addSuggestion(data: any) {
        this.onChooseAuthority.emit(data.originalObject);
    }

    addAny(data: string) {
        const authority = new Authority();
        authority.authorityName = data;
        authority.authorityType = RestConstants.AUTHORITY_TYPE_UNKNOWN;
        this.onChooseAuthority.emit(authority);
    }

    onSubmit() {
        if (this.allowAny) {
            this.addAny(this.inputValue);
        }
    }

    setOption(event: MatAutocompleteSelectedEvent) {
        console.log('set option', event);
        this.addSuggestion(event.option.value);
        this.suggestionGroups = null;
        setTimeout(() => (this.inputValue = ''));
    }

    updateSuggestions() {
        const value = this.inputValue;
        console.log('new event', JSON.stringify(this.inputValue));
        this.suggestionGroups = null;
        if (value.length < 2) {
            if (this.showRecent) {
                this.iam.getRecentlyInvited().subscribe(authorities => {
                    this.suggestionGroups = [
                        {
                            label: 'WORKSPACE.INVITE_RECENT_AUTHORITIES',
                            values: [],
                        },
                    ];
                    this.convertData(
                        this.suggestionGroups[0].values,
                        authorities.authorities,
                    );
                });
            }
            return;
        }
        this.suggestionGroups = [
            { label: 'WORKSPACE.INVITE_LOCAL_RESULTS', values: [] },
        ];
        if (this.mode == AuthoritySearchMode.UsersAndGroups) {
            if (this.globalSearchAllowed) {
                this.suggestionGroups.push({
                    label: 'WORKSPACE.INVITE_GLOBAL_RESULTS',
                    values: [],
                });
            }
            this.iam
                .searchAuthorities(value, false, this.groupType)
                .subscribe((authorities: IamAuthorities) => {
                    if (this.inputValue != value) return;
                    this.convertData(
                        this.suggestionGroups[0].values,
                        authorities.authorities,
                    );
                    if (authorities.authorities.length == 0)
                        this.suggestionGroups.splice(0, 1);
                    if (this.globalSearchAllowed) {
                        this.iam
                            .searchAuthorities(value, true, this.groupType)
                            .subscribe((authorities2: IamAuthorities) => {
                                if (this.inputValue != value) return;
                                // leave out all local existing persons
                                authorities2.authorities = authorities2.authorities.filter(
                                    authority =>
                                        Helper.indexOfObjectArray(
                                            authorities.authorities,
                                            'authorityName',
                                            authority.authorityName,
                                        ) == -1,
                                );
                                this.convertData(
                                    this.suggestionGroups[
                                        this.suggestionGroups.length - 1
                                    ].values,
                                    authorities2.authorities,
                                );
                                if (authorities2.authorities.length == 0)
                                    this.suggestionGroups.splice(1, 1);
                            });
                    }
                });
        }
        if (this.mode == AuthoritySearchMode.Organizations) {
            this.organization.getOrganizations(value).subscribe(orgs => {
                if (this.inputValue != value) return;
                this.convertData(
                    this.suggestionGroups[0].values,
                    orgs.organizations,
                );
            });
        }
    }

    showSuggestions() {
        if (!this.inputValue) {
            this.updateSuggestions();
        }
    }

    private convertData(
        suggestionGroup: any,
        authorities: AuthorityProfile[] | Group[],
    ) {
        for (const user of authorities) {
            const group = user.profile.displayName != null;
            const item = new SuggestItem(
                user.authorityName,
                group
                    ? user.profile.displayName
                    : NodeHelper.getUserDisplayName(user as AuthorityProfile),
                group ? 'group' : 'person',
                '',
            );
            item.originalObject = user;
            item.secondaryTitle = this.namePipe.transform(user, {
                field: 'secondary',
            });
            suggestionGroup.push(item);
        }
    }
}

export enum AuthoritySearchMode {
    UsersAndGroups = 'UsersAndGroups',
    Organizations = 'Organizations',
}
