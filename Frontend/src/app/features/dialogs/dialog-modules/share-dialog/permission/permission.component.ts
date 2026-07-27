import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { AuthenticationService } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../../core-module/core.module';
import { ExtendedAce } from '../share-dialog.component';
import { TypeResult } from '../choose-type/choose-type.component';
import { fromBeforeToValidator, notInPastValidator } from './timebased.validators';

@Component({
    selector: 'es-share-dialog-permission',
    templateUrl: 'permission.component.html',
    styleUrls: ['permission.component.scss'],
    standalone: false,
})
export class ShareDialogPermissionComponent implements OnInit {
    private authenticationService = inject(AuthenticationService);

    public _permission: ExtendedAce = null;
    public invalidPermission = false;
    public isEveryone: boolean;
    permissionTimebased: boolean;
    readonly timebasedForm = new FormGroup(
        {
            from: new FormControl<number | null>(
                { value: null, disabled: true },
                { validators: [notInPastValidator] },
            ),
            to: new FormControl<number | null>({ value: null, disabled: true }),
        },
        { validators: [fromBeforeToValidator] },
    );

    async ngOnInit() {
        this.permissionTimebased = await this.authenticationService.hasToolpermission(
            RestConstants.TOOLPERMISSION_INVITE_TIMEBASED,
        );
    }

    @Input() set permission(permission: ExtendedAce) {
        this._permission = permission;
        let coordinator = permission.permissions.indexOf(RestConstants.PERMISSION_COORDINATOR);
        let collaborator = permission.permissions.indexOf(RestConstants.PERMISSION_COLLABORATOR);
        if (coordinator != -1) {
            let i = permission.permissions.indexOf(RestConstants.PERMISSION_COLLABORATOR);
            if (i != -1) permission.permissions.splice(i, 1);
        }
        if (coordinator != -1 || collaborator != -1) {
            let i = permission.permissions.indexOf(RestConstants.PERMISSION_CONSUMER);
            if (i != -1) permission.permissions.splice(i, 1);
        }
        this.isEveryone = permission.authority.authorityName == RestConstants.AUTHORITY_EVERYONE;
        let check = this._permission.permissions.slice();
        if (check.indexOf(RestConstants.ACCESS_CC_PUBLISH) != -1) {
            check.splice(check.indexOf(RestConstants.ACCESS_CC_PUBLISH), 1);
        }
        this.invalidPermission =
            check.length != 1 ||
            (check[0] != RestConstants.PERMISSION_OWNER &&
                check[0] != RestConstants.PERMISSION_CONSUMER &&
                check[0] != RestConstants.PERMISSION_COLLABORATOR &&
                check[0] != RestConstants.PERMISSION_COORDINATOR);
    }
    @Input() inherit = false;
    @Input() isEditable: boolean | 'timebasedOnly' = false;
    @Input() showDelete = true;
    @Input() isDeleted = false;
    @Input() isDirectory = false;
    @Input() canPublish = true;
    @Input() timebasedAllowed = true;
    @Input() timebasedInvalid = false;
    @Output() removePermission = new EventEmitter<void>();
    @Output() changeType = new EventEmitter<TypeResult>();

    public showChooseType = false;
    timebasedOpen = false;
    currentDate = new Date().getTime();

    public remove() {
        if (this.showDelete) this.removePermission.emit();
    }
    public chooseType() {
        if (this.isEditable !== true || this.isEveryone) return;
        this.showChooseType = true;
    }
    doChangeType(type: TypeResult) {
        this.changeType.emit(type);
        if (type.wasMain) this.showChooseType = false;
    }

    getDateTomorrow(from?: number) {
        return Math.max(from || new Date().getTime()) + 1000 * 60 * 60 * 24;
    }

    get fromEnabled(): boolean {
        return this.timebasedForm.controls.from.enabled;
    }
    get toEnabled(): boolean {
        return this.timebasedForm.controls.to.enabled;
    }
    /** Current from/to values including disabled controls (used for the datepicker bounds). */
    get timebasedRaw(): { from: number | null; to: number | null } {
        return this.timebasedForm.getRawValue();
    }

    openTimebased() {
        this.setTimebasedControl('from', this._permission.from ?? null);
        this.setTimebasedControl('to', this._permission.to ?? null);
        this.timebasedForm.markAsPristine();
        this.timebasedOpen = true;
    }

    toggleFrom(checked: boolean) {
        this.setTimebasedControl('from', checked ? this.getDateTomorrow() : null, true);
    }
    toggleTo(checked: boolean) {
        this.setTimebasedControl(
            'to',
            checked ? this.getDateTomorrow(this.timebasedRaw.from) : null,
            true,
        );
    }

    private setTimebasedControl(name: 'from' | 'to', value: number | null, markDirty = false) {
        const control = this.timebasedForm.controls[name];
        control.setValue(value);
        if (value != null) {
            control.enable();
        } else {
            control.disable();
        }
        if (markDirty) {
            control.markAsDirty();
        }
    }

    onTimebasedKeydown(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            event.stopPropagation();
            this.timebasedOpen = false;
        }
    }

    saveTimebased() {
        if (this.timebasedForm.invalid) {
            this.timebasedForm.markAllAsTouched();
            return;
        }
        const { from, to } = this.timebasedForm.getRawValue();
        this._permission.from = from;
        this._permission.to = to;
        this.timebasedOpen = false;
    }
}
