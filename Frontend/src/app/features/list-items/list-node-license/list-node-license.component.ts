import { Component } from '@angular/core';
import { ListWidget } from '../list-widget';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { AccessibilityService } from 'src/app/common/ui/accessibility/accessibility.service';
import { ListItem, RestConstants, Node } from 'src/app/core-module/core.module';
import { NodeHelperService } from 'src/app/core-ui-module/node-helper.service';

@Component({
    selector: 'es-list-node-license',
    templateUrl: './list-node-license.component.html',
})
export class ListNodeLicenseComponent extends ListWidget {
    static supportedItems = [new ListItem('NODE', RestConstants.CCM_PROP_LICENSE)];

    readonly licenseName$ = this.nodeSubject.pipe(
        map((node) => this.nodeHelper.getLicenseName(node as Node)),
    );

    readonly licenseIcon$ = this.nodeSubject.pipe(
        map((node) => this.nodeHelper.getLicenseIcon(node as Node)),
    );

    readonly tooltip$ = rxjs.combineLatest([this.licenseName$, this.provideLabelSubject]).pipe(
        switchMap(([licenseName, provideLabel]) => {
            if (provideLabel) {
                return this.translate
                    .get('NODE.ccm:commonlicense_key')
                    .pipe(map((commonLicenseKey) => `${commonLicenseKey}: ${licenseName}`));
            } else {
                return rxjs.of(licenseName);
            }
        }),
    );

    readonly indicatorIcons$ = this.accessibility.observe('indicatorIcons');

    constructor(
        private accessibility: AccessibilityService,
        private nodeHelper: NodeHelperService,
        private translate: TranslateService,
    ) {
        super();
    }
}
