/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, Input } from '@angular/core';

@Component({
    selector: 'es-global-progress',
    templateUrl: 'global-progress.component.html',
    styleUrls: ['global-progress.component.scss'],
    standalone: false,
})
export class GlobalProgressComponent {
    @Input() message: string;
    constructor() {}
}
