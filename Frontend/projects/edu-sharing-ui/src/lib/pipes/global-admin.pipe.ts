import { inject, Injectable, Pipe, PipeTransform } from '@angular/core';
import { AuthenticationService } from 'ngx-edu-sharing-api';
import { map, take } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
@Pipe({
    name: 'esGlobalAdmin',
})
export class GlobalAdminPipe implements PipeTransform {
    private authenticationService = inject(AuthenticationService);

    transform(_value?: unknown) {
        return this.authenticationService.observeLoginInfo().pipe(
            map((login) => login.isAdmin),
            take(1),
        );
    }
}
