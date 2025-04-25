import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AboutService } from './wrappers/about.service';
import { AuthenticationService } from './wrappers/authentication.service';
import { RestConstants } from './rest-constants';

/**
 * provides information about the env and available features like external apis
 */
@Injectable({
    providedIn: 'root',
})
export class FeaturesHelperService {
    constructor(private auth: AuthenticationService, private about: AboutService) {}

    /**
     * returns true if the current user can access ai features
     * Requires necessary permissions as well as an connected & available api
     */
    async hasUserAISupport() {
        return (
            (await this.auth.hasToolpermission(RestConstants.TOOLPERMISSION_BAPI)) &&
            !!(await firstValueFrom(this.about.getAbout()))?.plugins?.find((f) => f.id === 'b-api')
        );
    }
}
