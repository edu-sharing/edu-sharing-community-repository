/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'es-header',
    templateUrl: 'header.component.html',
    styleUrls: ['header.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class SearchHeaderComponent {
    @Input() scope: string;
    constructor() {}
}
