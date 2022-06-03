import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable, Subject } from 'rxjs';
import { first, map, startWith, switchMap, take, tap } from 'rxjs/operators';
import { User, UserEntry, UserProfileEdit } from '../api/models';
import { IamV1Service } from '../api/services';
import { HOME_REPOSITORY, ME } from '../constants';
import { switchReplay } from '../utils/switch-replay';
import { AuthenticationService, LoginInfo } from './authentication.service';

export { UserEntry, User };

export interface CurrentUserInfo {
    user: UserEntry | null;
    loginInfo: LoginInfo;
}

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
     *
     * Might return `null` when not logged in.
     */
    getUser(userId: string, repository: string = HOME_REPOSITORY): Observable<UserEntry | null> {
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
     *
     * Might return `null` when not logged in.
     */
    observeCurrentUser(): Observable<UserEntry | null> {
        return this.currentUser$;
    }

    /**
     * Like `observeCurrentUser`, but also includes `loginInfo`.
     */
    observeCurrentUserInfo(): Observable<CurrentUserInfo> {
        return this.currentUser$.pipe(
            // Usually updated `loginInfo` will trigger an update of `currentUser`, so we should be
            // fine just taking whatever value `loginInfo` has at the time `currentUser` changes.
            switchMap((user) =>
                this.authentication.observeLoginInfo().pipe(
                    take(1),
                    map((loginInfo) => ({ user, loginInfo })),
                ),
            ),
        );
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
                if (userId === ME || userId === userEntry?.person.authorityName) {
                    return inner.pipe(tap(() => this.currentUserProfileChangesSubject.next()));
                } else {
                    return inner;
                }
            }),
        );
    }

    private createCurrentUser(): Observable<UserEntry | null> {
        return rxjs
            .merge(this.authentication.observeUserChanges(), this.currentUserProfileChangesSubject)
            .pipe(
                startWith(void 0 as void),
                switchMap(() => this.authentication.observeLoginInfo().pipe(take(1))),
                switchReplay((loginInfo) => {
                    if (loginInfo.isValidLogin) {
                        return this.getUserInner(ME, HOME_REPOSITORY);
                    } else {
                        return rxjs.of(null);
                    }
                }),
            );
    }

    private getUserInner(userId: string, repository: string): Observable<UserEntry> {
        return this.iamApi.getUser({ repository, person: userId });
    }
}
