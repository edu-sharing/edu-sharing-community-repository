import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Group, User } from '../../../../core-module/rest/data-object';
import { RestIamService, RestOrganizationService } from '../../../../core-module/core.module';
import { PermissionNamePipe } from '../../../../core-ui-module/pipes/permission-name.pipe';

@Component({
    selector: 'es-authority-row',
    templateUrl: 'authority-row.component.html',
    styleUrls: ['authority-row.component.scss'],
})
export class AuthorityRowComponent {
    @Input() authority: User | Group | any;
    @Input() secondaryTitle: string;

    constructor(
        private iam: RestIamService,
        private organization: RestOrganizationService,
        private namePipe: PermissionNamePipe,
    ) {}
}
