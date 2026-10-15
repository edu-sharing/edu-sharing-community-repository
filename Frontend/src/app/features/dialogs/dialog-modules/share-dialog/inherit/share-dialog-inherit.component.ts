import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatCheckbox } from '@angular/material/checkbox';
import type { ExtendedAce } from '../share-dialog.component';

/**
 * Inheritance setting of a node: a checkbox to toggle the inheritance plus the list of the
 * permissions that are inherited (see `getInheritedPermissions`).
 */
@Component({
    selector: 'es-share-dialog-inherit',
    templateUrl: './share-dialog-inherit.component.html',
    styleUrls: ['./share-dialog-inherit.component.scss'],
    standalone: false,
})
export class ShareDialogInheritComponent {
    /** the permissions that are inherited */
    @Input() permissions: ExtendedAce[] = [];
    /** the permissions of the parent could not be fetched */
    @Input() accessDenied = false;
    /** use the collection specific labels */
    @Input() isCollection = false;
    @Input() isDirectory = false;
    @Input() inherited: boolean;
    @Output() inheritedChange = new EventEmitter<boolean>();
    /** click on the checkbox, before its state changes (e.g. to veto unchecking it) */
    @Output() checkboxClick = new EventEmitter<MatCheckbox>();

    onInheritedChange(inherited: boolean): void {
        this.inherited = inherited;
        this.inheritedChange.emit(inherited);
    }
}
