import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, startWith, take } from 'rxjs/operators';
import { UserEntry, User } from '../api/models';
import { IamV1Service } from '../api/services';
import { HOME_REPOSITORY, ME } from '../constants';
import { switchRelay } from '../utils/switch-relay';
import { AuthenticationService } from './authentication.service';

export { UserEntry, User };

@Injectable({
    providedIn: 'root',
})
export class UserService {
    private readonly currentUser$ = this.createCurrentUser();

    constructor(private authentication: AuthenticationService, private iamApi: IamV1Service) {}

    /**
     * Returns the user with the given `userId`.
     *
     * When requesting a user other then `ME`, this method triggers an API request.
     */
    getUser(userId: string, repository: string): Observable<UserEntry> {
        if (userId === ME && repository === HOME_REPOSITORY) {
            return this.currentUser$.pipe(take(1));
        } else {
            return this.getUserInner(userId, repository);
        }
    }

    /**
     * Returns the currently signed in user.
     *
     * The observable will be updated when the user sings in or out.
     *
     * Subscribing to the observable does not necessarily trigger an API request, when we already
     * have fetched the data.
     */
    getCurrentUser(): Observable<UserEntry> {
        return this.currentUser$;
    }

    private createCurrentUser(): Observable<UserEntry> {
        return this.authentication.getUserChanges().pipe(
            startWith(void 0 as void),
            switchRelay(() => this.getUserInner(ME, HOME_REPOSITORY)),
        );
    }

    private getUserInner(userId: string, repository: string): Observable<UserEntry> {
        return this.iamApi.getUser({ repository, person: userId });
    }
}
