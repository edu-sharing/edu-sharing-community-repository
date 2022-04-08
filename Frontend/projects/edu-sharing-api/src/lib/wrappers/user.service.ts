import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable, Subject } from 'rxjs';
import { first, startWith, switchMap, take, tap } from 'rxjs/operators';
import { User, UserEntry, UserProfileEdit } from '../api/models';
import { IamV1Service } from '../api/services';
import { HOME_REPOSITORY, ME } from '../constants';
import { switchReplay } from '../utils/switch-replay';
import { AuthenticationService } from './authentication.service';

export { UserEntry, User };

@Injectable({
    providedIn: 'root',
})
export class UserService {
    /**
     * Triggers when the profile of the current user is edited.
     *
     * Does not trigger if the user is logged in or out.
     */
    private readonly currentUserProfileChangesSubject = new Subject<void>();
    /** The currently logged in user. */
    private readonly currentUser$ = this.createCurrentUser();

    constructor(private authentication: AuthenticationService, private iamApi: IamV1Service) {}

    /**
     * Returns the user with the given `userId`.
     *
     * When requesting a user other then `ME`, this method triggers an API request.
     */
    getUser(userId: string, repository: string = HOME_REPOSITORY): Observable<UserEntry> {
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
    observeCurrentUser(): Observable<UserEntry> {
        return this.currentUser$;
    }

    editProfile(
        userId: string,
        profile: UserProfileEdit,
        repository: string = HOME_REPOSITORY,
    ): Observable<void> {
        const inner = this.iamApi.changeUserProfile({
            person: userId,
            repository,
            body: profile,
        });
        return this.observeCurrentUser().pipe(
            first(),
            switchMap((userEntry) => {
                if (userId === ME || userId === userEntry.person.authorityName) {
                    return inner.pipe(tap(() => this.currentUserProfileChangesSubject.next()));
                } else {
                    return inner;
                }
            }),
        );
    }

    private createCurrentUser(): Observable<UserEntry> {
        return rxjs
            .merge(this.authentication.observeUserChanges(), this.currentUserProfileChangesSubject)
            .pipe(
                startWith(void 0 as void),
                switchReplay(() => this.getUserInner(ME, HOME_REPOSITORY)),
            );
    }

    private getUserInner(userId: string, repository: string): Observable<UserEntry> {
        return this.iamApi.getUser({ repository, person: userId });
    }
}
