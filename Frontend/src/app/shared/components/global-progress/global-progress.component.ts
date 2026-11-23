/**
 * Created by Torsten on 13.01.2017.
 */

import { Component } from '@angular/core';

/**
 * Legacy: Do not use `<es-global-progress>` directly in templates.
 * Use `Toast.showProgressSpinner()` / `Toast.closeProgressSpinner()` instead,
 * which renders this component inside a CDK overlay.
 */
@Component({
    selector: 'es-global-progress',
    templateUrl: 'global-progress.component.html',
    styleUrls: ['global-progress.component.scss'],
    standalone: false,
})
export class GlobalProgressComponent {
    constructor() {}
}
