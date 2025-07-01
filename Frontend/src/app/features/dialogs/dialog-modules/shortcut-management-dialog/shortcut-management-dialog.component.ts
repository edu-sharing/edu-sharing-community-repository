import { Component, Inject, OnDestroy, signal, ViewContainerRef } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { Subject } from 'rxjs';
import { SharedModule } from '../../../../shared/shared.module';
import { ShortcutEntriesComponent } from '../../../shortcut-entries/shortcut-entries.component';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { ShortcutManagementDialogData } from './shortcut-management-dialog-data';

@Component({
    selector: 'es-shortcut-management-dialog',
    imports: [ShortcutEntriesComponent, SharedModule],
    templateUrl: './shortcut-management-dialog.component.html',
    styleUrls: ['./shortcut-management-dialog.component.scss'],
})
export class ShortcutManagementDialogComponent implements OnDestroy {
    node = signal<Node>(null);
    constructor(@Inject(CARD_DIALOG_DATA) public data: ShortcutManagementDialogData) {
        this.node.set(data.node);
    }

    readonly destroyed$ = new Subject<void>();

    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
