import { Injectable, Pipe, PipeTransform, inject } from '@angular/core';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';

export type LicenseType = 'name';

@Injectable({ providedIn: 'root' })
@Pipe({
    name: 'esNodeLicense',
    standalone: false,
})
export class NodeLicensePipe implements PipeTransform {
    private translate = inject(TranslateService);

    transform(node: Node, args: { type: LicenseType }): Observable<string> {
        if (node.properties[RestConstants.CCM_PROP_LICENSE]?.[0]) {
            if (args?.type === 'name') {
                return this.translate.get(
                    'LICENSE.NAMES.' + node.properties[RestConstants.CCM_PROP_LICENSE]?.[0],
                );
            }
        }
        if (args?.type === 'name') {
            return this.translate.get('LICENSE.NAMES.NONE');
        }
        return of(null);
    }
}
