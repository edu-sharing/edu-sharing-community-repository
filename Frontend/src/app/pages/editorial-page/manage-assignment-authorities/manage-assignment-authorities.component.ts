import { Component, computed, input, model } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';
import { MatSelectChange } from '@angular/material/select';
import { AssignmentBase } from '../manage-assignment/manage-assignment.component';
import { AuthenticationService, Permission } from 'ngx-edu-sharing-api';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';

type Role = 'ASSIGNEE' | 'COORDINATOR';

@Component({
    selector: 'es-manage-assignment-authorities',
    templateUrl: 'manage-assignment-authorities.component.html',
    styleUrls: ['manage-assignment-authorities.component.scss'],
    imports: [SharedModule],
})
export class ManageAssignmentAuthoritiesComponent {
    assignment = input.required<AssignmentBase>();
    authorities = model.required<Permission[]>();
    loginInfo = toSignal(this.authenticationService.observeLoginInfo());
    authoritiesFiltered = computed(() =>
        this.authorities().filter(
            (a) => a.authority?.authorityName !== this.loginInfo().authorityName,
        ),
    );
    readonly translateReady$ = new BehaviorSubject<boolean>(false);

    constructor(
        private translate: TranslateService,
        private authenticationService: AuthenticationService,
        public nodeHelperService: NodeHelperService,
    ) {
        // dirty hack for https://github.com/angular/components/issues/7923
        this.translate.get('ANY').subscribe(() => this.translateReady$.next(true));
    }
    remove(item: Permission) {
        this.authorities().splice(this.authorities().indexOf(item), 1);
        this.authorities.set(this.authorities());
    }

    protected readonly NodesRightMode = NodesRightMode;

    setRole(item: Permission, $event: MatSelectChange<Role>) {
        item.role = $event.value;
        this.authorities.set(this.authorities());
    }
}
