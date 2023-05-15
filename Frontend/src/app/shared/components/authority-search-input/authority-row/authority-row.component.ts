import { Component, Input } from '@angular/core';
import { Group, User } from '../../../../core-module/rest/data-object';

@Component({
    selector: 'es-authority-row',
    templateUrl: 'authority-row.component.html',
    styleUrls: ['authority-row.component.scss'],
})
export class AuthorityRowComponent {
    @Input() authority: User | Group | any;
    @Input() secondaryTitle: string;

    constructor() {}
}
