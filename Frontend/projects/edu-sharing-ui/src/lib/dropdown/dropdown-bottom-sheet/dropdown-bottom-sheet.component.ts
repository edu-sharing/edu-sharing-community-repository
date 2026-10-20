import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MAT_BOTTOM_SHEET_DATA, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { OptionItem } from '../../types/option-item';
import { BehaviorSubject } from 'rxjs';
import { Node } from 'ngx-edu-sharing-api';
import { Helper } from '../../util/helper';

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

    /** The options whose submenus were opened, from the outermost to the currently shown one. */
    readonly parents = signal<OptionItem[]>([]);
    readonly currentParent = computed(() => this.parents().at(-1) ?? null);
    private readonly rootOptions = toSignal(this.options$, { initialValue: [] as OptionItem[] });
    /** The options of the menu level that is currently shown. */
    readonly visibleOptions = computed(() => {
        const parent = this.currentParent();
        // Menus are nested at most one level deep, so submenu entries cannot open further menus.
        return parent
            ? parent.subOptions.filter((o) => !o.subOptions?.length)
            : this.rootOptions() ?? [];
    });
    readonly selectable = computed(() => !!this.currentParent()?.subOptionsSelectable);

    click(option: OptionItem): void {
        if (!option.isEnabled) {
            return;
        }
        if (option.subOptions?.length) {
            this.parents.update((parents) => [...parents, option]);
            return;
        }
        this._bottomSheetRef.dismiss();
        setTimeout(() => option.callback(null, this.callbackObjects));
    }

    back(): void {
        this.parents.update((parents) => parents.slice(0, -1));
    }

    /** Click on a link option; modified clicks are left to the browser. */
    clickLink(option: OptionItem, event: MouseEvent): void {
        if (!option.isEnabled || Helper.isModifiedClick(event)) {
            return;
        }
        this._bottomSheetRef.dismiss();
        if (!option.link.routerLink) {
            event.preventDefault();
        }
        setTimeout(() => option.callback(null, this.callbackObjects));
    }

    isNewGroup(i: number) {
        const options = this.visibleOptions();
        if (i > 0) {
            return options[i].group !== options[i - 1].group;
        }
        return false;
    }
}
