import { Injectable } from '@angular/core';
import { RegisterV1Service } from '../api/services/register-v-1.service';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthenticationService } from './authentication.service';
import { RegisterExists } from '../api/models/register-exists';
import { RegisterInformation } from '../api/models/register-information';

@Injectable({
    providedIn: 'root',
})
export class RegisterService {
    constructor(
        private registerV1Service: RegisterV1Service,
        private authenticationService: AuthenticationService,
    ) {}

    /**
     * Activate a new user (by using a supplied key).
     *
     * This method triggers a login validation afterward, as activating the user should create
     * a valid session.
     * @param key The key for the user to activate
     */
    activate(key: string): Observable<any> {
        return this.registerV1Service
            .activate({ key })
            .pipe(tap(() => this.authenticationService.forceLoginInfoRefresh()));
    }

    /**
     * Check if the given mail is already successfully registered.
     *
     * @param mail The mail (authority) of the user to check
     */
    mailExists(mail: string): Observable<RegisterExists> {
        return this.registerV1Service.mailExists({ mail });
    }

    /**
     * Send a mail to recover/reset password.
     *
     * @param mail The mail (authority) of the user to recover
     */
    recoverPassword(mail: string): Observable<any> {
        return this.registerV1Service.recoverPassword({ mail });
    }

    /**
     * Register a new user.
     */
    register(body: RegisterInformation): Observable<any> {
        return this.registerV1Service.register({ body });
    }

    /**
     * Resend a registration mail for a given mail address.
     *
     * The method will return false if there is no pending registration for the given mail
     *
     * @param mail The mail a registration is pending for and should be resent to
     */
    resendMail(mail: string): Observable<any> {
        return this.registerV1Service.resendMail({ mail });
    }

    /**
     * Send a mail to recover/reset password.
     *
     * @param key The key for the password reset request
     * @param password the new password for the user
     */
    resetPassword(key: string, password: string): Observable<any> {
        return this.registerV1Service.resetPassword({ key: key, password: password });
    }
}
