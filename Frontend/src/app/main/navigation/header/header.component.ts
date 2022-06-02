/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, Input } from '@angular/core';

@Component({
    selector: 'es-header',
    templateUrl: 'header.component.html',
    styleUrls: ['header.component.scss'],
})
export class SearchHeaderComponent {
    @Input() scope: string;
    constructor() {}
}
