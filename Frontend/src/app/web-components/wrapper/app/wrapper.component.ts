import { Component, Input, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService } from 'ngx-edu-sharing-api';

@Component({
    selector: 'app-root',
    templateUrl: './wrapper.component.html',
    styleUrls: ['./wrapper.component.scss'],
    standalone: false,
})
export class WrapperComponent implements OnInit {
    private router = inject(Router);
    /**
     * service to allow access for sending authentication data
     */
    authenticationService = inject(AuthenticationService);

    @Input()
    get searchString(): string {
        return this._searchString;
    }
    set searchString(value: string) {
        this._searchString = value;
        this.onSearchStringChanged();
    }
    private _searchString: string;

    @Input()
    get ticket(): string {
        return this._ticket;
    }
    set ticket(value: string) {
        this._ticket = value;
        this.onTicketStringChanged();
    }
    private _ticket: string;

    ngOnInit(): void {
        // We need this to hook up routing to our LocationStrategy. Otherwise calls on Location
        // won't work.
        this.router.initialNavigation();
        this.goToSearch();
    }

    private onSearchStringChanged() {
        this.goToSearch(this.searchString);
    }

    private goToSearch(searchString?: string) {
        void this.router.navigate(['/components/search'], {
            queryParams: {
                mainnav: false,
                q: searchString,
            },
        });
    }

    private onTicketStringChanged() {
        this.authenticationService.loginEduTicket(this.ticket);
    }
}
