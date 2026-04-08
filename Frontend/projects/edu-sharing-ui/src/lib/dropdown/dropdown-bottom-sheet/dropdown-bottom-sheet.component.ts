import { Component, inject } from '@angular/core';
import { MAT_BOTTOM_SHEET_DATA, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { OptionItem } from '../../types/option-item';
import { BehaviorSubject } from 'rxjs';
import { Node } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-dropdown-bottom-sheet',
    templateUrl: 'dropdown-bottom-sheet.component.html',
    styleUrls: ['dropdown-bottom-sheet.component.scss'],
    standalone: false,
})
export class DropdownBottomSheetComponent {
    private readonly data = inject<{
        options$: BehaviorSubject<OptionItem[]>;
        callbackObjects: Node[];
        showDisabled: boolean;
    }>(MAT_BOTTOM_SHEET_DATA);
    readonly options$ = this.data.options$;
    readonly callbackObjects = this.data.callbackObjects;
    readonly showDisabled = this.data.showDisabled;
    _bottomSheetRef = inject<MatBottomSheetRef<DropdownBottomSheetComponent>>(MatBottomSheetRef);

    click(option: OptionItem): void {
        this._bottomSheetRef.dismiss();
        if (!option.isEnabled) {
            return;
        }
        setTimeout(() => option.callback(null, this.callbackObjects));
    }
    isNewGroup(i: number) {
        if (i > 0) {
            return this.options$.value[i].group !== this.options$.value[i - 1].group;
        }
        return false;
    }
}
